package dev.pimon.forum.dto;

import java.time.LocalDateTime;

/** Hilo completo (detalle): metadatos + datos de la categoría. Los mensajes van paginados aparte. */
public record ThreadDto(
        String        id,
        String        slug,
        String        title,
        String        categoryId,
        String        categorySlug,
        String        categoryName,
        String        authorName,
        boolean       authorIsAdmin,
        boolean       pinned,
        boolean       locked,
        int           replyCount,
        long          viewCount,
        LocalDateTime createdAt
) {}
