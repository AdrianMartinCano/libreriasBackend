package dev.pimon.forum.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sección del foro. La crea y ordena el admin; los usuarios abren hilos dentro.
 *
 * Se llama {@code ForumCategory} (y no {@code Category}) para no chocar con la
 * entidad {@code Category} de spring-products (nombre de entidad/bean único).
 */
@Entity
@Table(name = "forum_categories", indexes = {
        @Index(name = "idx_forum_cat_position", columnList = "position")
})
@Getter @Setter @NoArgsConstructor
public class ForumCategory extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    /** Orden en la portada del foro (menor primero). */
    private int position = 0;

    /** Icono opcional (nombre/emoji) para la portada. */
    private String icon;

    /** Color opcional (hex) para la portada. */
    private String color;
}
