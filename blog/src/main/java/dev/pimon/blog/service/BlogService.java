package dev.pimon.blog.service;

import dev.pimon.blog.dto.*;
import dev.pimon.blog.entity.Comment;
import dev.pimon.blog.entity.Post;
import dev.pimon.blog.repository.CommentRepository;
import dev.pimon.blog.repository.PostRepository;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.security.model.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class BlogService {

    private final PostRepository    postRepo;
    private final CommentRepository commentRepo;
    private final java.util.Optional<CommentAuthorNameResolver> nameResolver;

    public BlogService(PostRepository postRepo, CommentRepository commentRepo,
                       java.util.Optional<CommentAuthorNameResolver> nameResolver) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.nameResolver = nameResolver;
    }

    // ── Público ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDto> listPublished(Pageable pageable) {
        return PageResponse.from(
                postRepo.findByPublishedTrueOrderByPublishedAtDesc(pageable).map(BlogService::toSummary));
    }

    @Transactional(readOnly = true)
    public PostDto getPublishedBySlug(String slug) {
        return toDto(postRepo.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> AppException.notFound("Entrada no encontrada: " + slug)));
    }

    @Transactional(readOnly = true)
    public List<CommentDto> comments(String slug) {
        Post post = postRepo.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> AppException.notFound("Entrada no encontrada: " + slug));
        List<Comment> list = commentRepo.findByPostIdOrderByCreatedAtAsc(post.getId());
        // Nick EN VIVO: resolvemos el nombre actual por autor (una vez por id), así
        // cambiar el nick en el perfil se refleja en todos sus comentarios. Si no se
        // puede resolver, cae al nombre guardado al comentar.
        java.util.Map<String, String> liveNames = new java.util.HashMap<>();
        return list.stream().map(c -> {
            String live = liveNames.computeIfAbsent(c.getAuthorId(),
                    id -> nameResolver.map(r -> r.resolve(id)).orElse(null));
            String name = (live != null && !live.isBlank()) ? live.trim() : c.getAuthorName();
            return new CommentDto(c.getId(), name, c.isAuthorIsAdmin(), c.getBody(), c.getCreatedAt());
        }).toList();
    }

    /** Añade un comentario. Requiere usuario autenticado. */
    public CommentDto addComment(String slug, CreateCommentRequest req, AuthUser user) {
        if (user == null || user.id() == null) {
            throw AppException.unauthorized("Debes iniciar sesión para comentar");
        }
        Post post = postRepo.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> AppException.notFound("Entrada no encontrada: " + slug));

        Comment c = new Comment();
        c.setPostId(post.getId());
        c.setAuthorId(user.id());
        c.setAuthorName(resolveName(user));
        c.setAuthorIsAdmin(user.isAdmin());
        c.setBody(req.body().trim());
        return toCommentDto(commentRepo.save(c));
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryDto> listAll(Pageable pageable) {
        return PageResponse.from(postRepo.findAllByOrderByCreatedAtDesc(pageable).map(BlogService::toSummary));
    }

    @Transactional(readOnly = true)
    public PostDto getById(String id) {
        return toDto(findById(id));
    }

    /**
     * Crea una entrada y la firma con quien la escribe.
     *
     * El autor se guarda <b>al crear y solo al crear</b>: que alguien entre a corregir una
     * errata no lo convierte en el autor de la entrada. Si no hay usuario —una llamada
     * interna, una siembra— queda sin firmar, que es mejor que firmarla a nombre de nadie.
     */
    public PostDto create(SavePostRequest req, AuthUser user) {
        Post p = new Post();
        p.setSlug(uniqueSlug(toSlug(req.title())));
        p.setAuthorId(user != null ? user.id() : null);
        apply(p, req);
        return toDto(postRepo.save(p));
    }

    public PostDto update(String id, SavePostRequest req) {
        Post p = findById(id);
        apply(p, req);
        return toDto(postRepo.save(p));
    }

    public void delete(String id) {
        Post p = findById(id);
        commentRepo.findByPostIdOrderByCreatedAtAsc(id).forEach(commentRepo::delete);
        postRepo.delete(p);
    }

    /** Borra un comentario (moderación de admin). */
    public void deleteComment(String commentId) {
        if (!commentRepo.existsById(commentId)) {
            throw AppException.notFound("Comentario no encontrado: " + commentId);
        }
        commentRepo.deleteById(commentId);
    }

    // ── Privados ──────────────────────────────────────────────────────────

    /** Vuelca el request en la entidad y gestiona la fecha de publicación. */
    private void apply(Post p, SavePostRequest req) {
        p.setTitle(req.title());
        p.setExcerpt(req.excerpt());
        p.setBody(req.body());
        p.setCoverImage(req.coverImage());
        if (req.tags() != null) p.setTags(req.tags());
        boolean wasPublished = p.isPublished();
        p.setPublished(req.published());
        // Fija publishedAt la primera vez que se publica.
        if (req.published() && (!wasPublished || p.getPublishedAt() == null)) {
            p.setPublishedAt(LocalDateTime.now());
        }
        if (!req.published()) {
            p.setPublishedAt(null);
        }
    }

    private Post findById(String id) {
        return postRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Entrada no encontrada: " + id));
    }

    private String uniqueSlug(String base) {
        String slug = base.isBlank() ? "entrada" : base;
        String candidate = slug;
        int i = 1;
        while (postRepo.existsBySlug(candidate)) {
            candidate = slug + "-" + i++;
        }
        return candidate;
    }

    /** Nick a mostrar: el del perfil (resolver server-side) o, si no, la parte local del email. */
    private String resolveName(AuthUser user) {
        String resolved = nameResolver.map(r -> r.resolve(user.id())).orElse(null);
        if (resolved != null && !resolved.isBlank()) return resolved.trim();
        String email = user.email();
        if (email == null || email.isBlank()) return "Usuario";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static String toSlug(String text) {
        if (text == null) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static PostDto toDto(Post p) {
        return new PostDto(p.getId(), p.getSlug(), p.getTitle(), p.getExcerpt(), p.getBody(),
                p.getCoverImage(), p.getTags(), p.isPublished(), p.getPublishedAt(), p.getCreatedAt());
    }

    private static PostSummaryDto toSummary(Post p) {
        return new PostSummaryDto(p.getId(), p.getSlug(), p.getTitle(), p.getExcerpt(),
                p.getCoverImage(), p.getTags(), p.isPublished(), p.getPublishedAt());
    }

    private static CommentDto toCommentDto(Comment c) {
        return new CommentDto(c.getId(), c.getAuthorName(), c.isAuthorIsAdmin(),
                c.getBody(), c.getCreatedAt());
    }
}
