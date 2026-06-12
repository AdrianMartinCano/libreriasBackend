package dev.pimon.newsletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_campaigns")
@Getter
@Setter
@NoArgsConstructor
public class NewsletterCampaign extends BaseEntity {

    @Column(nullable = false)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private int recipientCount;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}
