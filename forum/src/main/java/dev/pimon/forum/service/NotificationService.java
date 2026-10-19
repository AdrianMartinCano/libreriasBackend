package dev.pimon.forum.service;

import dev.pimon.common.dto.PageResponse;
import dev.pimon.forum.dto.NotificationDto;
import dev.pimon.forum.entity.ForumThread;
import dev.pimon.forum.entity.Notification;
import dev.pimon.forum.entity.NotificationType;
import dev.pimon.forum.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avisos del foro. Lo llaman los demás servicios para emitir, y el controller
 * para que el usuario lea su bandeja.
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repo;

    /** Conserva solo las N notificaciones más recientes por usuario; 0 = no podar (deja todas). */
    private final int maxPerUser;

    public NotificationService(NotificationRepository repo,
            @org.springframework.beans.factory.annotation.Value("${pimon.forum.max-notifications-per-user:0}") int maxPerUser) {
        this.repo = repo;
        this.maxPerUser = maxPerUser;
    }

    /** Emite un aviso. No notifica si el destinatario es el propio actor. */
    public void notify(String recipientId, String actorId, String actorName,
                       NotificationType type, ForumThread thread, String postId) {
        if (recipientId == null || recipientId.equals(actorId)) return;
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setActorName(actorName);
        n.setType(type);
        n.setThreadId(thread.getId());
        n.setThreadSlug(thread.getSlug());
        n.setThreadTitle(thread.getTitle());
        n.setPostId(postId);
        repo.save(n);
        prune(recipientId);
    }

    /**
     * Conserva solo las {@code maxPerUser} notificaciones más recientes del usuario,
     * borrando las que sobren. Con {@code forum.max-notifications-per-user = 0} no hace nada
     * (la bandeja crece sin tope, comportamiento por defecto).
     */
    private void prune(String recipientId) {
        if (maxPerUser <= 0) return;
        java.util.List<Notification> all =
                repo.findByRecipientIdOrderByCreatedAtDesc(recipientId, Pageable.unpaged()).getContent();
        if (all.size() > maxPerUser) {
            repo.deleteAll(all.subList(maxPerUser, all.size()));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(String userId, Pageable pageable) {
        return PageResponse.from(
                repo.findByRecipientIdOrderByCreatedAtDesc(userId, pageable).map(NotificationService::toDto));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repo.countByRecipientIdAndReadFalse(userId);
    }

    public void markRead(String userId, String id) {
        repo.findById(id)
                .filter(n -> n.getRecipientId().equals(userId))
                .ifPresent(n -> { n.setRead(true); repo.save(n); });
    }

    public void markAllRead(String userId) {
        repo.findByRecipientIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .forEach(n -> { if (!n.isRead()) { n.setRead(true); repo.save(n); } });
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType().name(), n.getActorName(),
                n.getThreadSlug(), n.getThreadTitle(), n.getPostId(), n.isRead(), n.getCreatedAt());
    }
}
