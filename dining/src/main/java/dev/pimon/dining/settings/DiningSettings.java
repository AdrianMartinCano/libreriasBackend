package dev.pimon.dining.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Ajustes de reservas de restaurante, editables en caliente desde el panel. Se persisten como JSON en
 * {@code app_settings} bajo la clave "dining"; los defaults salen de {@code pimon.dining.*}.
 *
 * <p>Los turnos no están aquí: son entidades {@link dev.pimon.dining.entity.Seating} (más ricas, con
 * días y duración). Estos ajustes son las restricciones globales del servicio.</p>
 */
public record DiningSettings(

        /** Aforo total en comensales por turno. 0 = sin tope global (manda la capacidad de la mesa). */
        @PositiveOrZero int totalCapacity,

        /** Antelación máxima (días) con la que se admite una reserva. */
        @Min(1) int maxAdvanceDays,

        /** Antelación mínima (horas) antes del inicio del turno. */
        @PositiveOrZero int minAdvanceHours,

        /** Días cerrados (festivos/vacaciones) en ISO 'YYYY-MM-DD'. Puede ser vacío. */
        List<String> blockedDates
) {}
