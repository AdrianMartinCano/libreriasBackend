package dev.pimon.reservations.dto;

import dev.pimon.reservations.settings.TimeRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * Alta/edición de un trabajador desde el panel del jefe. La coherencia {@code end > start} de cada
 * tramo se valida en {@code StaffService} (igual que hace {@code ReservationsSettingsService}).
 */
public record SaveStaffRequest(
        @NotBlank String name,
        String color,
        String imageUrl,
        boolean active,
        int sortOrder,
        /** Id del AppUser a vincular (opcional). null/vacío = trabajador sin login. */
        String userId,
        boolean customHours,
        @Valid Map<DayOfWeek, List<@Valid TimeRange>> weeklyHours,
        /** Vacaciones propias en ISO 'YYYY-MM-DD'. */
        List<@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato: YYYY-MM-DD") String> blockedDates,
        /** Servicios que realiza. Vacío/null = hace todos. */
        List<String> serviceIds
) {}
