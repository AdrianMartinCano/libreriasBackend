package dev.pimon.dining.dto;

import dev.pimon.dining.entity.Seating;

import java.time.DayOfWeek;
import java.util.Set;

public record SeatingDto(
        String         id,
        String         name,
        String         start,
        String         end,
        int            durationMinutes,
        Set<DayOfWeek> daysOfWeek,
        int            sortOrder,
        boolean        active
) {
    public static SeatingDto from(Seating s) {
        return new SeatingDto(
                s.getId(), s.getName(), s.getStart(), s.getEnd(), s.getDurationMinutes(),
                s.getDaysOfWeek(), s.getSortOrder(), s.isActive());
    }
}
