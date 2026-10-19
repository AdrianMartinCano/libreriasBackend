package dev.pimon.reservations.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Un tramo horario de un día (p. ej. 09:00–13:00). Un día puede tener varios (turno partido).
 * Formato 'HH:mm'. La coherencia {@code end > start} la valida {@code ReservationsSettingsService}.
 */
public record TimeRange(
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "Formato HH:mm") String start,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "Formato HH:mm") String end
) {}
