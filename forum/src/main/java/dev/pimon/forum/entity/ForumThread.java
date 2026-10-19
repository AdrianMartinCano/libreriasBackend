package dev.pimon.forum.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Hilo/tema del foro. Lo abre un usuario dentro de una categoría. El cuerpo del
 * mensaje de apertura vive en un {@link ForumPost} con {@code opening=true}, así
 * reacciones, citas y edición funcionan igual para apertura y respuestas.
 *
 * Se llama {@code ForumThread} (y no {@code Thread}) para no chocar con
 * {@link java.lang.Thread}.
 */
@Entity
@Table(name = "forum_threads", indexes = {
        @Index(name = "idx_forum_thread_category", columnList = "categoryId"),
        @Index(name = "idx_forum_thread_activity", columnList = "pinned, lastPostAt")
})
@Getter @Setter @NoArgsConstructor
public class ForumThread extends BaseEntity {

    @Column(nullable = false)
    private String categoryId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String authorName;

    /** true si el autor era administrador al abrir el hilo (para el distintivo). */
    private boolean authorIsAdmin = false;

    /** Fijado arriba por el admin. */
    private boolean pinned = false;

    /** Cerrado: no admite más respuestas. */
    private boolean locked = false;

    /** Respuestas (mensajes sin contar el de apertura). Denormalizado. */
    private int replyCount = 0;

    /** Visitas al detalle. */
    private long viewCount = 0;

    /** Fecha del último mensaje (ordena la lista). Se fija al abrir el hilo. */
    private LocalDateTime lastPostAt;

    /** Nick del autor del último mensaje (para mostrar en el listado). */
    private String lastPostAuthorName;
}
