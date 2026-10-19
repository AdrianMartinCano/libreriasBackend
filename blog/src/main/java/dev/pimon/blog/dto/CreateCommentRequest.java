package dev.pimon.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 2000, message = "El comentario es demasiado largo")
        String body
) {}
