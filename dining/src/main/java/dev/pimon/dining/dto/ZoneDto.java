package dev.pimon.dining.dto;

import dev.pimon.dining.entity.Zone;

public record ZoneDto(String id, String name, int sortOrder, boolean active) {
    public static ZoneDto from(Zone z) {
        return new ZoneDto(z.getId(), z.getName(), z.getSortOrder(), z.isActive());
    }
}
