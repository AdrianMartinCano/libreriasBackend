package dev.pimon.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editar el cuerpo de un mensaje propio. */
public record UpdatePostRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 20000, message = "El mensaje es demasiado largo")
        String body
) {}
