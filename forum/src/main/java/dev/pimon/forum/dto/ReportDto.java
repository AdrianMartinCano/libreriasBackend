package dev.pimon.forum.dto;

import java.time.LocalDateTime;

/** Denuncia para la cola de moderación del admin. */
public record ReportDto(
        String        id,
        String        postId,
        String        threadId,
        String        threadSlug,
        String        reporterName,
        String        reason,
        String        status,
        String        postPreview,
        LocalDateTime createdAt
) {}
