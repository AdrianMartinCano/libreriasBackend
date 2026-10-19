package dev.pimon.blog.dto;

import java.time.LocalDateTime;
import java.util.Set;

/** Post resumido (para el feed/listado). */
public record PostSummaryDto(
        String        id,
        String        slug,
        String        title,
        String        excerpt,
        String        coverImage,
        Set<String>   tags,
        boolean       published,
        LocalDateTime publishedAt
) {}
