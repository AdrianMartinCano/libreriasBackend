package dev.pimon.products.dto;

import java.time.LocalDateTime;

public record ProductDto(
        String      id,
        String      name,
        String      slug,
        String      description,
        double      price,
        Double      originalPrice,
        Integer     discount,        // % calculado automáticamente
        int         stock,
        boolean     inStock,
        String      imageUrl,
        String      badge,
        CategoryDto category,
        LocalDateTime createdAt
) {}
