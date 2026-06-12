package dev.pimon.cms.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/** Crear o actualizar una página CMS (mismo DTO para ambas operaciones) */
public record SaveCmsPageRequest(
        @NotBlank(message = "El slug es obligatorio")
        String slug,

        @NotBlank(message = "El título es obligatorio")
        String title,

        String description,

        /**
         * Array de bloques. Cada bloque debe tener al menos "type" y "data".
         * Ejemplo:
         * [
         *   { "type": "hero", "data": { "title": "Bienvenido", "cta": { "label": "Ver más", "href": "/productos" } } },
         *   { "type": "features", "data": { "cols": 3, "items": [...] } }
         * ]
         */
        List<Map<String, Object>> blocks,

        boolean published
) {}
