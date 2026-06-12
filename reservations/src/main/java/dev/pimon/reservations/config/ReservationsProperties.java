package dev.pimon.reservations.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración en application.yml:
 *
 * libui:
 *   reservations:
 *     work-start: "09:00"
 *     work-end: "20:00"
 *     work-days: [TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY]
 *     slot-interval-minutes: 30
 */
@Data
@ConfigurationProperties(prefix = "pimon.reservations")
public class ReservationsProperties {

    private String workStart = "09:00";
    private String workEnd   = "20:00";

    /** Días de la semana en los que hay disponibilidad (nombre en inglés, mayúsculas). */
    private List<String> workDays = List.of(
            "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
    );

    /** Cada cuántos minutos se genera un slot de disponibilidad. */
    private int slotIntervalMinutes = 30;
}
