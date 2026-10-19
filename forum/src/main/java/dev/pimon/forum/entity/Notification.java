package dev.pimon.forum.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aviso para un usuario: alguien respondió en su hilo, le citó o reaccionó a su
 * mensaje.
 */
@Entity
@Table(name = "forum_notifications", indexes = {
        @Index(name = "idx_forum_notif_recipient", columnList = "recipientId, read, createdAt")
})
@Getter @Setter @NoArgsConstructor
public class Notification extends BaseEntity {

    /** Usuario que recibe el aviso. */
    @Column(nullable = false)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /** Quien provocó el aviso (nick a mostrar). */
    @Column(nullable = false)
    private String actorName;

    /** Hilo relacionado (para enlazar). */
    @Column(nullable = false)
    private String threadId;

    private String threadSlug;

    private String threadTitle;

    /** Mensaje relacionado (la respuesta/cita/reacción), o null. */
    private String postId;

    private boolean read = false;
}
