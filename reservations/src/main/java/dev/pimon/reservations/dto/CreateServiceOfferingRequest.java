package dev.pimon.reservations.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateServiceOfferingRequest(
        @NotBlank String name,
        String description,
        @Min(5) int durationMinutes,
        @PositiveOrZero BigDecimal price,
        String imageUrl,
        String color
) {}
