package dev.pimon.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responder en un hilo. {@code quotedPostId} opcional (cita). */
public record CreatePostRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 20000, message = "El mensaje es demasiado largo")
        String body,
        String quotedPostId
) {}
