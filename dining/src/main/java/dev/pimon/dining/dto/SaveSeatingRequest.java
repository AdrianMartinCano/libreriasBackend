package dev.pimon.dining.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.DayOfWeek;
import java.util.Set;

/** Alta/edición de turno. {@code end > start} lo valida el servicio. */
public record SaveSeatingRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "Formato HH:mm") String start,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "Formato HH:mm") String end,
        @Positive int durationMinutes,
        @NotEmpty Set<DayOfWeek> daysOfWeek,
        @PositiveOrZero int sortOrder
) {}
