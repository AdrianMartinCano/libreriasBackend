package dev.pimon.blog.dto;

import java.time.LocalDateTime;

public record CommentDto(
        String        id,
        String        authorName,
        boolean       authorIsAdmin,
        String        body,
        LocalDateTime createdAt
) {}
