package dev.pimon.menu.dto;

import dev.pimon.menu.entity.CategoryIcon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Alta/edición de categoría de la carta. El {@code slug} lo genera el servicio a partir del nombre. */
public record CreateMenuCategoryRequest(
        @NotBlank String name,
        String description,
        CategoryIcon icon,
        @PositiveOrZero int sortOrder
) {}
