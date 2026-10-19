package dev.pimon.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Crear/editar una categoría (admin). */
public record SaveCategoryRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,
        @Size(max = 500, message = "La descripción es demasiado larga")
        String description,
        int    position,
        String icon,
        String color
) {}
