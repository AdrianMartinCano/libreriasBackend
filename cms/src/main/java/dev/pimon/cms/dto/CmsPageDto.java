package dev.pimon.cms.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Respuesta pública de una página CMS.
 * Los bloques vienen deserializados como List<Map> — el frontend
 * los pasa directamente a <lib-cms-page [page]="..."/>.
 */
public record CmsPageDto(
        String                    id,
        String                    slug,
        String                    title,
        String                    description,
        List<Map<String, Object>> blocks,
        LocalDateTime             updatedAt
) {}
