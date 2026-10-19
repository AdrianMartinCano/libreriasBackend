package dev.pimon.dining.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Alta/edición de mesa. La coherencia {@code capacityMin <= capacityMax} la valida el servicio. */
public record SaveTableRequest(
        @NotBlank String label,
        @Min(1) int capacityMin,
        @Min(1) int capacityMax,
        boolean combinable,
        @PositiveOrZero int sortOrder,
        Integer posX,
        Integer posY,
        @NotBlank String zoneId
) {}
