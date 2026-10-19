package dev.pimon.forum.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Mensaje dentro de un hilo. El primer mensaje (apertura) lleva {@code opening=true}.
 */
@Entity
@Table(name = "forum_posts", indexes = {
        @Index(name = "idx_forum_post_thread", columnList = "threadId, createdAt")
})
@Getter @Setter @NoArgsConstructor
public class ForumPost extends BaseEntity {

    @Column(nullable = false)
    private String threadId;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String authorName;

    /** true si el autor era administrador al escribir (para el distintivo). */
    private boolean authorIsAdmin = false;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /** true si es el mensaje de apertura del hilo (no se puede borrar suelto). */
    private boolean opening = false;

    /** Id del mensaje citado (cita), o null. */
    private String quotedPostId;

    /** Fecha de la última edición, o null si no se ha editado. */
    private LocalDateTime editedAt;
}
