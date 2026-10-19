package dev.pimon.forum.dto;

/** Categoría del foro, con contadores para la portada. */
public record CategoryDto(
        String id,
        String slug,
        String name,
        String description,
        int    position,
        String icon,
        String color,
        long   threadCount
) {}
