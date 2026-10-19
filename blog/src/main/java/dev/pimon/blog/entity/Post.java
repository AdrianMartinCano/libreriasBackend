package dev.pimon.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entrada del blog (feed cronológico). La escribe el admin; el público la lee.
 */
@Entity
@Table(name = "blog_posts", indexes = {
        @Index(name = "idx_blog_published", columnList = "published, publishedAt")
})
@Getter @Setter @NoArgsConstructor
public class Post extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String slug;

    /**
     * Quién la escribió.
     *
     * <b>Puede ser nulo</b>, y no por descuido: las entradas anteriores a este campo no
     * guardaron autor, y no hay forma de averiguarlo a posteriori. Quien las lea tiene que
     * aguantar el nulo en vez de inventarse un dueño.
     *
     * No se guarda el nombre, solo el id: el nombre se resuelve en vivo desde el perfil
     * —igual que en los comentarios y en el foro— y así cambiar de nick no deja entradas
     * firmadas con el nombre viejo.
     */
    private String authorId;

    @Column(nullable = false)
    private String title;

    /** Resumen para el listado. */
    @Column(length = 600)
    private String excerpt;

    /** Cuerpo del post (HTML/markdown/texto). */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body = "";

    private String coverImage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "blog_post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    @BatchSize(size = 50)
    private Set<String> tags = new HashSet<>();

    /** false = borrador (no visible públicamente). */
    private boolean published = false;

    /** Fecha de publicación (se fija al publicar). Ordena el feed. */
    private LocalDateTime publishedAt;
}
