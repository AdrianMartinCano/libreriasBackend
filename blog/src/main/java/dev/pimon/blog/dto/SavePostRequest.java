package dev.pimon.blog.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/** Crear/editar un post (admin). */
public record SavePostRequest(
        @NotBlank(message = "El título es obligatorio")
        String      title,
        String      excerpt,
        @NotBlank(message = "El contenido es obligatorio")
        String      body,
        String      coverImage,
        Set<String> tags,
        boolean     published
) {}
