package dev.pimon.newsletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    // Sin @Lob: con Postgres, @Lob sobre String trata la columna como Large Object (oid) y
    // exige transacción al leerla ("Large Objects may not be used in auto-commit mode").
    // La columna ya es TEXT, así que se mapea como string normal.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private int recipientCount;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}
