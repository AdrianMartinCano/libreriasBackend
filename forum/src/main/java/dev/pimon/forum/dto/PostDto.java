package dev.pimon.forum.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mensaje de un hilo. Incluye la cita resuelta (si la hay) y el resumen de
 * reacciones (conteo por tipo + cuáles ha puesto el usuario actual).
 */
public record PostDto(
        String              id,
        String              threadId,
        String              authorId,
        String              authorName,
        boolean             authorIsAdmin,
        String              body,
        boolean             opening,
        QuotedPostDto       quoted,
        List<ReactionCount> reactions,
        LocalDateTime       editedAt,
        LocalDateTime       createdAt
) {
    /** Vista mínima del mensaje citado. */
    public record QuotedPostDto(String id, String authorName, String body) {}

    /** Conteo de un tipo de reacción y si el usuario actual la ha puesto. */
    public record ReactionCount(String type, long count, boolean reacted) {}
}
