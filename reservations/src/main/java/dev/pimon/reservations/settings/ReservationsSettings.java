package dev.pimon.reservations.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * Ajustes de reservas editables en caliente desde el panel de administración. Se persisten como JSON
 * en {@code app_settings} bajo la clave "reservations"; los valores por defecto salen de
 * {@code pimon.reservations.*} en {@code application.yml}.
 *
 * <p><b>Horario por día con turnos partidos:</b> {@code weeklyHours} mapea cada día de la semana a su
 * lista de tramos (p. ej. lunes → [09:00–13:00, 16:00–20:00]). Un día <b>ausente o con lista vacía</b>
 * = cerrado. Permite sábado reducido, horario partido, etc.</p>
 */
public record ReservationsSettings(

        /** Día de la semana → tramos abiertos. Día ausente o vacío = cerrado. */
        @Valid
        Map<DayOfWeek, List<@Valid TimeRange>> weeklyHours,

        @Positive
        int slotIntervalMinutes,

        /** Reservas que pueden solaparse a la vez (recursos en paralelo). */
        @Positive
        int capacity,

        /**
         * Días concretos cerrados (vacaciones, festivos) en ISO 'YYYY-MM-DD'. No se aceptan reservas
         * en estas fechas aunque sea día laborable. Puede ser vacío.
         */
        List<String> blockedDates
) {}
