package dev.pimon.forum.service;

import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.forum.dto.*;
import dev.pimon.forum.dto.PostDto.QuotedPostDto;
import dev.pimon.forum.dto.PostDto.ReactionCount;
import dev.pimon.forum.entity.ForumCategory;
import dev.pimon.forum.entity.ForumPost;
import dev.pimon.forum.entity.ForumThread;
import dev.pimon.forum.entity.NotificationType;
import dev.pimon.forum.repository.ForumCategoryRepository;
import dev.pimon.forum.repository.ForumPostRepository;
import dev.pimon.forum.repository.ThreadRepository;
import dev.pimon.security.model.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Núcleo del foro: categorías, hilos, mensajes y búsqueda. Reacciones,
 * denuncias y notificaciones viven en sus propios servicios.
 */
@Service
@Transactional
public class ForumService {

    private final ForumCategoryRepository categoryRepo;
    private final ThreadRepository    threadRepo;
    private final ForumPostRepository postRepo;
    private final ReactionService     reactions;
    private final ReportService       reports;
    private final NotificationService notifications;
    private final ForumNames          names;

    /** Minutos que un usuario puede editar su mensaje tras publicarlo. 0 = sin límite. El admin no tiene límite. */
    private final long editWindowMinutes;

    public ForumService(ForumCategoryRepository categoryRepo, ThreadRepository threadRepo,
                        ForumPostRepository postRepo, ReactionService reactions,
                        ReportService reports, NotificationService notifications, ForumNames names,
                        @org.springframework.beans.factory.annotation.Value("${pimon.forum.edit-window-minutes:15}") long editWindowMinutes) {
        this.categoryRepo  = categoryRepo;
        this.threadRepo    = threadRepo;
        this.postRepo      = postRepo;
        this.reactions     = reactions;
        this.reports       = reports;
        this.notifications = notifications;
        this.names         = names;
        this.editWindowMinutes = editWindowMinutes;
    }

    // ── Categorías (lectura pública) ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        return categoryRepo.findAllByOrderByPositionAscNameAsc().stream()
                .map(c -> toCategoryDto(c, threadRepo.countByCategoryId(c.getId())))
                .toList();
    }

    // ── Hilos (lectura pública) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ThreadSummaryDto> listThreads(String categorySlug, Pageable pageable) {
        ForumCategory cat = categoryRepo.findBySlug(categorySlug)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + categorySlug));
        return PageResponse.from(
                threadRepo.findByCategoryIdOrderByPinnedDescLastPostAtDesc(cat.getId(), pageable)
                        .map(t -> toThreadSummary(t, cat.getSlug())));
    }

    @Transactional(readOnly = true)
    public PageResponse<ThreadSummaryDto> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return PageResponse.from(Page.empty(pageable));
        String term = q.strip();
        // Hilos por título + hilos cuyos mensajes contienen el término, sin duplicar.
        Map<String, ForumThread> found = new LinkedHashMap<>();
        threadRepo.findByTitleContainingIgnoreCaseOrderByLastPostAtDesc(term, pageable)
                .forEach(t -> found.put(t.getId(), t));
        postRepo.findByBodyContainingIgnoreCaseOrderByCreatedAtDesc(term, pageable).forEach(p ->
                found.computeIfAbsent(p.getThreadId(), id -> threadRepo.findById(id).orElse(null)));
        List<ThreadSummaryDto> items = found.values().stream()
                .filter(Objects::nonNull)
                .map(t -> toThreadSummary(t, categorySlugOf(t)))
                .toList();
        return PageResponse.from(new org.springframework.data.domain.PageImpl<>(items, pageable, items.size()));
    }

    /** Detalle del hilo. Incrementa visitas. */
    public ThreadDto getThread(String slug) {
        ForumThread t = threadRepo.findBySlug(slug)
                .orElseThrow(() -> AppException.notFound("Hilo no encontrado: " + slug));
        t.setViewCount(t.getViewCount() + 1);
        threadRepo.save(t);
        ForumCategory cat = categoryRepo.findById(t.getCategoryId()).orElse(null);
        return toThreadDto(t, cat);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostDto> listPosts(String threadSlug, Pageable pageable, AuthUser user) {
        ForumThread t = threadRepo.findBySlug(threadSlug)
                .orElseThrow(() -> AppException.notFound("Hilo no encontrado: " + threadSlug));
        Page<ForumPost> page = postRepo.findByThreadIdOrderByCreatedAtAsc(t.getId(), pageable);
        return PageResponse.from(toPostDtos(page, user == null ? null : user.id()));
    }

    // ── Escritura (usuarios autenticados) ───────────────────────────────────

    /** Abre un hilo: crea el hilo y su mensaje de apertura. */
    public ThreadDto openThread(String categorySlug, CreateThreadRequest req, AuthUser user) {
        AuthUser u = requireUser(user);
        ForumCategory cat = categoryRepo.findBySlug(categorySlug)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + categorySlug));

        String authorName = names.forWriting(u);
        ForumThread t = new ForumThread();
        t.setCategoryId(cat.getId());
        t.setSlug(uniqueThreadSlug(toSlug(req.title())));
        t.setTitle(req.title().strip());
        t.setAuthorId(u.id());
        t.setAuthorName(authorName);
        t.setAuthorIsAdmin(u.isAdmin());
        t.setLastPostAt(LocalDateTime.now());
        t.setLastPostAuthorName(authorName);
        threadRepo.save(t);

        ForumPost opening = new ForumPost();
        opening.setThreadId(t.getId());
        opening.setAuthorId(u.id());
        opening.setAuthorName(authorName);
        opening.setAuthorIsAdmin(u.isAdmin());
        opening.setBody(req.body().strip());
        opening.setOpening(true);
        postRepo.save(opening);

        return toThreadDto(t, cat);
    }

    /** Responde en un hilo. Admite cita. Notifica a autor del hilo y del mensaje citado. */
    public PostDto reply(String threadSlug, CreatePostRequest req, AuthUser user) {
        AuthUser u = requireUser(user);
        ForumThread t = threadRepo.findBySlug(threadSlug)
                .orElseThrow(() -> AppException.notFound("Hilo no encontrado: " + threadSlug));
        if (t.isLocked() && !u.isAdmin()) {
            throw AppException.forbidden("El hilo está cerrado");
        }

        ForumPost quoted = null;
        if (req.quotedPostId() != null && !req.quotedPostId().isBlank()) {
            quoted = postRepo.findById(req.quotedPostId())
                    .filter(p -> p.getThreadId().equals(t.getId()))
                    .orElseThrow(() -> AppException.badRequest("El mensaje citado no pertenece a este hilo"));
        }

        String authorName = names.forWriting(u);
        ForumPost post = new ForumPost();
        post.setThreadId(t.getId());
        post.setAuthorId(u.id());
        post.setAuthorName(authorName);
        post.setAuthorIsAdmin(u.isAdmin());
        post.setBody(req.body().strip());
        post.setQuotedPostId(quoted == null ? null : quoted.getId());
        postRepo.save(post);

        t.setReplyCount(t.getReplyCount() + 1);
        t.setLastPostAt(LocalDateTime.now());
        t.setLastPostAuthorName(authorName);
        threadRepo.save(t);

        // Avisos: al autor del hilo y, si se citó a otro distinto, también al citado.
        notifications.notify(t.getAuthorId(), u.id(), authorName, NotificationType.REPLY, t, post.getId());
        if (quoted != null && !quoted.getAuthorId().equals(t.getAuthorId())) {
            notifications.notify(quoted.getAuthorId(), u.id(), authorName, NotificationType.QUOTE, t, post.getId());
        }

        return toPostDto(post, quoted, reactions.summaryFor(post.getId(), u.id()));
    }

    /** Edita un mensaje propio. */
    public PostDto updateOwnPost(String postId, UpdatePostRequest req, AuthUser user) {
        AuthUser u = requireUser(user);
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> AppException.notFound("Mensaje no encontrado: " + postId));
        if (!post.getAuthorId().equals(u.id())) {
            throw AppException.forbidden("Solo puedes editar tus propios mensajes");
        }
        if (!u.isAdmin() && editWindowMinutes > 0
                && post.getCreatedAt().plusMinutes(editWindowMinutes).isBefore(LocalDateTime.now())) {
            throw AppException.forbidden(
                    "El plazo para editar este mensaje (" + editWindowMinutes + " min) ha expirado");
        }
        post.setBody(req.body().strip());
        post.setEditedAt(LocalDateTime.now());
        postRepo.save(post);
        ForumPost quoted = resolveQuoted(post.getQuotedPostId());
        return toPostDto(post, quoted, reactions.summaryFor(post.getId(), u.id()));
    }

    /** Borra un mensaje propio. Si es el de apertura, borra el hilo entero. */
    public void deleteOwnPost(String postId, AuthUser user) {
        AuthUser u = requireUser(user);
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> AppException.notFound("Mensaje no encontrado: " + postId));
        if (!post.getAuthorId().equals(u.id())) {
            throw AppException.forbidden("Solo puedes borrar tus propios mensajes");
        }
        if (post.isOpening()) {
            deleteThreadCascade(post.getThreadId());
        } else {
            deletePostCascade(post);
        }
    }

    // ── Admin: categorías ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryDto> adminListCategories() {
        return listCategories();
    }

    public CategoryDto createCategory(SaveCategoryRequest req) {
        ForumCategory c = new ForumCategory();
        c.setSlug(uniqueCategorySlug(toSlug(req.name())));
        apply(c, req);
        categoryRepo.save(c);
        return toCategoryDto(c, 0);
    }

    public CategoryDto updateCategory(String id, SaveCategoryRequest req) {
        ForumCategory c = categoryRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + id));
        apply(c, req);
        categoryRepo.save(c);
        return toCategoryDto(c, threadRepo.countByCategoryId(c.getId()));
    }

    /**
     * Borra una categoría. Si tiene hilos, se bloquea salvo que {@code force} sea
     * true, en cuyo caso se borra en cascada (hilos + mensajes + reacciones + denuncias).
     */
    public void deleteCategory(String id, boolean force) {
        ForumCategory c = categoryRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + id));
        if (threadRepo.countByCategoryId(id) > 0) {
            if (!force) {
                throw AppException.conflict(
                        "La categoría tiene hilos. Muévelos a otra categoría o usa force=true para borrarla con todo su contenido");
            }
            threadRepo.findByCategoryIdOrderByPinnedDescLastPostAtDesc(id, Pageable.unpaged())
                    .forEach(t -> deleteThreadCascade(t.getId()));
        }
        categoryRepo.delete(c);
    }

    /** Mueve un hilo a otra categoría (admin). */
    public void moveThread(String threadId, String targetCategoryId) {
        ForumThread t = requireThread(threadId);
        ForumCategory target = categoryRepo.findById(targetCategoryId)
                .orElseThrow(() -> AppException.notFound("Categoría destino no encontrada: " + targetCategoryId));
        t.setCategoryId(target.getId());
        threadRepo.save(t);
    }

    // ── Admin: moderación de hilos/mensajes ─────────────────────────────────

    public void setPinned(String threadId, boolean pinned) {
        ForumThread t = requireThread(threadId);
        t.setPinned(pinned);
        threadRepo.save(t);
    }

    public void setLocked(String threadId, boolean locked) {
        ForumThread t = requireThread(threadId);
        t.setLocked(locked);
        threadRepo.save(t);
    }

    public void deleteThread(String threadId) {
        requireThread(threadId);
        deleteThreadCascade(threadId);
    }

    /** Borra un mensaje (admin). Si es el de apertura, borra el hilo entero. */
    public void deletePost(String postId) {
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> AppException.notFound("Mensaje no encontrado: " + postId));
        if (post.isOpening()) {
            deleteThreadCascade(post.getThreadId());
        } else {
            deletePostCascade(post);
        }
    }

    // ── Privados ────────────────────────────────────────────────────────────

    private void deletePostCascade(ForumPost post) {
        reactions.deleteForPost(post.getId());
        reports.deleteForPost(post.getId());
        postRepo.delete(post);
        threadRepo.findById(post.getThreadId()).ifPresent(this::recomputeThreadStats);
    }

    private void deleteThreadCascade(String threadId) {
        postRepo.findByThreadIdOrderByCreatedAtAsc(threadId).forEach(p -> {
            reactions.deleteForPost(p.getId());
            reports.deleteForPost(p.getId());
        });
        postRepo.deleteByThreadId(threadId);
        threadRepo.deleteById(threadId);
    }

    /** Recalcula respuestas y último mensaje tras borrar un mensaje. */
    private void recomputeThreadStats(ForumThread t) {
        t.setReplyCount((int) postRepo.countByThreadIdAndOpeningFalse(t.getId()));
        postRepo.findFirstByThreadIdOrderByCreatedAtDesc(t.getId()).ifPresent(last -> {
            t.setLastPostAt(last.getCreatedAt());
            t.setLastPostAuthorName(names.forDisplay(last.getAuthorId(), last.getAuthorName()));
        });
        threadRepo.save(t);
    }

    private static void apply(ForumCategory c, SaveCategoryRequest req) {
        c.setName(req.name().strip());
        c.setDescription(req.description());
        c.setPosition(req.position());
        c.setIcon(req.icon());
        c.setColor(req.color());
    }

    private ForumPost resolveQuoted(String quotedPostId) {
        if (quotedPostId == null) return null;
        return postRepo.findById(quotedPostId).orElse(null);
    }

    private String categorySlugOf(ForumThread t) {
        return categoryRepo.findById(t.getCategoryId()).map(ForumCategory::getSlug).orElse(null);
    }

    private ForumThread requireThread(String id) {
        return threadRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Hilo no encontrado: " + id));
    }

    private static AuthUser requireUser(AuthUser user) {
        if (user == null || user.id() == null) {
            throw AppException.unauthorized("Debes iniciar sesión");
        }
        return user;
    }

    // ── Mapeo a DTOs ────────────────────────────────────────────────────────

    private Page<PostDto> toPostDtos(Page<ForumPost> page, String userId) {
        List<ForumPost> posts = page.getContent();
        List<String> ids = posts.stream().map(ForumPost::getId).toList();
        Map<String, List<ReactionCount>> reactionsByPost = reactions.summariesFor(ids, userId);

        // Resolver citas en bloque.
        List<String> quotedIds = posts.stream()
                .map(ForumPost::getQuotedPostId).filter(Objects::nonNull).distinct().toList();
        Map<String, ForumPost> quotedById = new HashMap<>();
        if (!quotedIds.isEmpty()) {
            postRepo.findAllById(quotedIds).forEach(p -> quotedById.put(p.getId(), p));
        }
        // Cache de nicks en vivo por autor.
        Map<String, String> liveNames = new HashMap<>();
        return page.map(p -> {
            ForumPost quoted = p.getQuotedPostId() == null ? null : quotedById.get(p.getQuotedPostId());
            return toPostDto(p, quoted, reactionsByPost.getOrDefault(p.getId(), List.of()), liveNames);
        });
    }

    private PostDto toPostDto(ForumPost p, ForumPost quoted, List<ReactionCount> reactionCounts) {
        return toPostDto(p, quoted, reactionCounts, new HashMap<>());
    }

    private PostDto toPostDto(ForumPost p, ForumPost quoted, List<ReactionCount> reactionCounts,
                             Map<String, String> liveNamesCache) {
        String name = liveNamesCache.computeIfAbsent(p.getAuthorId(),
                id -> names.forDisplay(id, p.getAuthorName()));
        QuotedPostDto quotedDto = quoted == null ? null : new QuotedPostDto(
                quoted.getId(),
                liveNamesCache.computeIfAbsent(quoted.getAuthorId(),
                        id -> names.forDisplay(id, quoted.getAuthorName())),
                quoted.getBody());
        return new PostDto(p.getId(), p.getThreadId(), p.getAuthorId(), name, p.isAuthorIsAdmin(),
                p.getBody(), p.isOpening(), quotedDto, reactionCounts, p.getEditedAt(), p.getCreatedAt());
    }

    private ThreadSummaryDto toThreadSummary(ForumThread t, String categorySlug) {
        return new ThreadSummaryDto(t.getId(), t.getSlug(), t.getTitle(), t.getCategoryId(), categorySlug,
                names.forDisplay(t.getAuthorId(), t.getAuthorName()), t.isAuthorIsAdmin(),
                t.isPinned(), t.isLocked(), t.getReplyCount(), t.getViewCount(),
                t.getLastPostAt(), t.getLastPostAuthorName(), t.getCreatedAt());
    }

    private ThreadDto toThreadDto(ForumThread t, ForumCategory cat) {
        return new ThreadDto(t.getId(), t.getSlug(), t.getTitle(), t.getCategoryId(),
                cat == null ? null : cat.getSlug(), cat == null ? null : cat.getName(),
                names.forDisplay(t.getAuthorId(), t.getAuthorName()), t.isAuthorIsAdmin(),
                t.isPinned(), t.isLocked(), t.getReplyCount(), t.getViewCount(), t.getCreatedAt());
    }

    private static CategoryDto toCategoryDto(ForumCategory c, long threadCount) {
        return new CategoryDto(c.getId(), c.getSlug(), c.getName(), c.getDescription(),
                c.getPosition(), c.getIcon(), c.getColor(), threadCount);
    }

    // ── Slugs ───────────────────────────────────────────────────────────────

    private String uniqueThreadSlug(String base) {
        String slug = base.isBlank() ? "hilo" : base;
        String candidate = slug;
        int i = 1;
        while (threadRepo.existsBySlug(candidate)) candidate = slug + "-" + i++;
        return candidate;
    }

    private String uniqueCategorySlug(String base) {
        String slug = base.isBlank() ? "categoria" : base;
        String candidate = slug;
        int i = 1;
        while (categoryRepo.existsBySlug(candidate)) candidate = slug + "-" + i++;
        return candidate;
    }

    private static String toSlug(String text) {
        if (text == null) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
