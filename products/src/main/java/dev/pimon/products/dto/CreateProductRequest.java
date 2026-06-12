package dev.pimon.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "La descripción es obligatoria")
        String description,

        @NotNull @Positive(message = "El precio debe ser mayor que 0")
        Double price,

        Double originalPrice,

        @NotNull @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        String imageUrl,
        String badge,

        @NotBlank(message = "La categoría es obligatoria")
        String categoryId
) {}
