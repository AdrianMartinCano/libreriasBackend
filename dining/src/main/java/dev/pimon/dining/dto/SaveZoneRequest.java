package dev.pimon.dining.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SaveZoneRequest(
        @NotBlank String name,
        @PositiveOrZero int sortOrder
) {}
