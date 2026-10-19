package dev.pimon.dining.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración en application.yml:
 *
 * pimon:
 *   dining:
 *     total-capacity: 0          # 0 = sin tope global de aforo (solo manda la mesa)
 *     max-advance-days: 60       # antelación máxima para reservar
 *     min-advance-hours: 1       # antelación mínima
 *     blocked-dates: []          # días cerrados ISO 'YYYY-MM-DD'
 *
 * Son los valores por defecto; el aforo/turnos/cierres son editables en caliente (SettingsStore).
 */
@Data
@ConfigurationProperties(prefix = "pimon.dining")
public class DiningProperties {

    /** Aforo total en comensales por turno. 0 = sin tope global (la restricción es la mesa). */
    private int totalCapacity = 0;

    /** Antelación máxima (días) con la que se admite una reserva. */
    private int maxAdvanceDays = 60;

    /** Antelación mínima (horas) antes del turno. */
    private int minAdvanceHours = 1;

    /** Días cerrados (festivos/vacaciones) en ISO 'YYYY-MM-DD'. */
    private List<String> blockedDates = List.of();
}
