package dev.pimon.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Abrir un hilo: título + cuerpo del mensaje de apertura. */
public record CreateThreadRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título es demasiado largo")
        String title,
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 20000, message = "El mensaje es demasiado largo")
        String body
) {}
