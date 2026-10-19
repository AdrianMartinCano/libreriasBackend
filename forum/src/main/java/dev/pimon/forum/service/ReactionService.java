package dev.pimon.forum.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.forum.dto.PostDto.ReactionCount;
import dev.pimon.forum.entity.*;
import dev.pimon.forum.repository.ForumPostRepository;
import dev.pimon.forum.repository.ReactionRepository;
import dev.pimon.forum.repository.ThreadRepository;
import dev.pimon.security.model.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Reacciones a mensajes. Poner/quitar (toggle) y construir el resumen por mensaje.
 */
@Service
@Transactional
public class ReactionService {

    private final ReactionRepository  reactionRepo;
    private final ForumPostRepository postRepo;
    private final ThreadRepository    threadRepo;
    private final NotificationService notifications;
    private final ForumNames          names;

    public ReactionService(ReactionRepository reactionRepo, ForumPostRepository postRepo,
                           ThreadRepository threadRepo, NotificationService notifications,
                           ForumNames names) {
        this.reactionRepo  = reactionRepo;
        this.postRepo      = postRepo;
        this.threadRepo    = threadRepo;
        this.notifications = notifications;
        this.names         = names;
    }

    /** Alterna una reacción del usuario sobre un mensaje y devuelve el resumen actualizado. */
    public List<ReactionCount> toggle(String postId, ReactionType type, AuthUser user) {
        if (user == null || user.id() == null) {
            throw AppException.unauthorized("Debes iniciar sesión para reaccionar");
        }
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> AppException.notFound("Mensaje no encontrado: " + postId));

        Optional<Reaction> existing = reactionRepo.findByPostIdAndUserIdAndType(postId, user.id(), type);
        if (existing.isPresent()) {
            reactionRepo.delete(existing.get());
        } else {
            Reaction r = new Reaction();
            r.setPostId(postId);
            r.setUserId(user.id());
            r.setType(type);
            reactionRepo.save(r);
            // Avisar al autor del mensaje (salvo que se reaccione a sí mismo).
            threadRepo.findById(post.getThreadId()).ifPresent(thread ->
                    notifications.notify(post.getAuthorId(), user.id(), names.forWriting(user),
                            NotificationType.REACTION, thread, postId));
        }
        return summaryFor(postId, user.id());
    }

    /** Resumen de reacciones de un mensaje para un usuario (puede ser null si anónimo). */
    @Transactional(readOnly = true)
    public List<ReactionCount> summaryFor(String postId, String userId) {
        return summarize(reactionRepo.findByPostId(postId), userId).getOrDefault(postId, List.of());
    }

    /** Resúmenes de varios mensajes de una tacada (evita N+1 al listar un hilo). */
    @Transactional(readOnly = true)
    public Map<String, List<ReactionCount>> summariesFor(List<String> postIds, String userId) {
        if (postIds.isEmpty()) return Map.of();
        return summarize(reactionRepo.findByPostIdIn(postIds), userId);
    }

    private Map<String, List<ReactionCount>> summarize(List<Reaction> reactions, String userId) {
        // postId -> (tipo -> conteo) y postId -> tipos del usuario actual.
        Map<String, Map<ReactionType, Long>> counts = new HashMap<>();
        Map<String, Set<ReactionType>> mine = new HashMap<>();
        for (Reaction r : reactions) {
            counts.computeIfAbsent(r.getPostId(), k -> new EnumMap<>(ReactionType.class))
                    .merge(r.getType(), 1L, Long::sum);
            if (userId != null && userId.equals(r.getUserId())) {
                mine.computeIfAbsent(r.getPostId(), k -> EnumSet.noneOf(ReactionType.class)).add(r.getType());
            }
        }
        Map<String, List<ReactionCount>> out = new HashMap<>();
        counts.forEach((postId, byType) -> {
            Set<ReactionType> userTypes = mine.getOrDefault(postId, Set.of());
            List<ReactionCount> list = new ArrayList<>();
            for (ReactionType t : ReactionType.values()) {
                long c = byType.getOrDefault(t, 0L);
                if (c > 0) list.add(new ReactionCount(t.name(), c, userTypes.contains(t)));
            }
            out.put(postId, list);
        });
        return out;
    }

    void deleteForPost(String postId) {
        reactionRepo.deleteByPostId(postId);
    }
}
