package dev.pimon.cms.dto;

import java.time.LocalDateTime;

/** Vista resumida para el listado (sin los bloques) */
public record CmsPageSummaryDto(
        String        id,
        String        slug,
        String        title,
        String        description,
        boolean       published,
        LocalDateTime updatedAt
) {}
