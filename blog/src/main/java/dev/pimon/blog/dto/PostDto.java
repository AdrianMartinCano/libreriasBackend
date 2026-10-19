package dev.pimon.blog.dto;

import java.time.LocalDateTime;
import java.util.Set;

/** Post completo (detalle). */
public record PostDto(
        String        id,
        String        slug,
        String        title,
        String        excerpt,
        String        body,
        String        coverImage,
        Set<String>   tags,
        boolean       published,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {}
