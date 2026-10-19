package dev.pimon.forum.dto;

import java.time.LocalDateTime;

/** Notificación del foro para la bandeja del usuario. */
public record NotificationDto(
        String        id,
        String        type,
        String        actorName,
        String        threadSlug,
        String        threadTitle,
        String        postId,
        boolean       read,
        LocalDateTime createdAt
) {}
