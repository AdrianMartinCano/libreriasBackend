package dev.pimon.newsletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscribers", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
public class NewsletterSubscriber extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    /** Origen del alta: 'home', 'footer', 'popup'... */
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriberStatus status = SubscriberStatus.PENDING;

    @Column(unique = true)
    private String confirmationToken;

    @Column(unique = true)
    private String unsubscribeToken;

    private LocalDateTime confirmedAt;

    private LocalDateTime unsubscribedAt;

    public enum SubscriberStatus {
        PENDING, CONFIRMED, UNSUBSCRIBED
    }
}
