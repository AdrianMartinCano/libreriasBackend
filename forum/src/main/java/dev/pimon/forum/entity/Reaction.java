package dev.pimon.forum.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reacción de un usuario a un mensaje. Única por (mensaje, usuario, tipo): el
 * usuario puede poner/quitar cada tipo una vez.
 */
@Entity
@Table(name = "forum_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_forum_reaction", columnNames = {"postId", "userId", "type"})
}, indexes = {
        @Index(name = "idx_forum_reaction_post", columnList = "postId")
})
@Getter @Setter @NoArgsConstructor
public class Reaction extends BaseEntity {

    @Column(nullable = false)
    private String postId;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReactionType type;
}
