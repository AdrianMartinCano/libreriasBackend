package dev.pimon.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

/**
 * Comentario de un post. Solo lo crean usuarios registrados.
 */
@Entity
@Table(name = "blog_comments", indexes = {
        @Index(name = "idx_blog_comment_post", columnList = "postId")
})
@Getter @Setter @NoArgsConstructor
public class Comment extends BaseEntity {

    @Column(nullable = false)
    private String postId;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String authorName;

    /** true si el autor era administrador al comentar (para mostrar distintivo). */
    private boolean authorIsAdmin = false;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;
}
