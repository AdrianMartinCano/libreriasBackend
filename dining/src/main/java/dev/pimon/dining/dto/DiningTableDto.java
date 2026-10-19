package dev.pimon.dining.dto;

import dev.pimon.dining.entity.DiningTable;

public record DiningTableDto(
        String  id,
        String  label,
        int     capacityMin,
        int     capacityMax,
        boolean combinable,
        int     sortOrder,
        boolean active,
        Integer posX,
        Integer posY,
        String  zoneId,
        String  zoneName
) {
    public static DiningTableDto from(DiningTable t) {
        return new DiningTableDto(
                t.getId(), t.getLabel(), t.getCapacityMin(), t.getCapacityMax(),
                t.isCombinable(), t.getSortOrder(), t.isActive(),
                t.getPosX(), t.getPosY(),
                t.getZone().getId(), t.getZone().getName());
    }
}
