package dev.pimon.forum.dto;

import java.time.LocalDateTime;

/** Hilo resumido (para listados y búsqueda). */
public record ThreadSummaryDto(
        String        id,
        String        slug,
        String        title,
        String        categoryId,
        String        categorySlug,
        String        authorName,
        boolean       authorIsAdmin,
        boolean       pinned,
        boolean       locked,
        int           replyCount,
        long          viewCount,
        LocalDateTime lastPostAt,
        String        lastPostAuthorName,
        LocalDateTime createdAt
) {}
