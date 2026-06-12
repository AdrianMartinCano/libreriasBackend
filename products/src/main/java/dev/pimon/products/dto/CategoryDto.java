package dev.pimon.products.dto;

public record CategoryDto(
        String id,
        String name,
        String slug,
        String description,
        String imageUrl
) {}
