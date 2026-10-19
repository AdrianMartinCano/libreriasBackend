package dev.pimon.forum.dto;

import dev.pimon.forum.entity.ReactionType;
import jakarta.validation.constraints.NotNull;

/** Alternar (poner/quitar) una reacción a un mensaje. */
public record ReactRequest(
        @NotNull(message = "El tipo de reacción es obligatorio")
        ReactionType type
) {}
