package dev.pimon.products.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        String description,
        String imageUrl
) {}
