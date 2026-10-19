package dev.pimon.forum.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Denuncia de un mensaje. La crea un usuario; el admin la resuelve.
 */
@Entity
@Table(name = "forum_reports", indexes = {
        @Index(name = "idx_forum_report_status", columnList = "status, createdAt")
})
@Getter @Setter @NoArgsConstructor
public class Report extends BaseEntity {

    @Column(nullable = false)
    private String postId;

    @Column(nullable = false)
    private String reporterId;

    @Column(nullable = false)
    private String reporterName;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;
}
