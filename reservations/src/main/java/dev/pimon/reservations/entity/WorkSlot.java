package dev.pimon.reservations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;

/**
 * Un tramo del horario propio de un trabajador (p. ej. lunes 09:00–13:00). Varias filas para el mismo
 * día = turno partido. Es la versión persistible de {@code settings.TimeRange} (que es un {@code record}
 * y no se puede mapear como {@code @Embeddable}); añade el día de la semana.
 *
 * <p>Solo se aplica si el {@link StaffMember} tiene {@code customHours = true}; en caso contrario el
 * trabajador hereda el horario del negocio ({@code ReservationsSettings.weeklyHours}).</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class WorkSlot {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;          // MONDAY…SUNDAY

    @Column(name = "start_time", nullable = false, length = 5)
    private String start;                 // "09:00"

    @Column(name = "end_time", nullable = false, length = 5)
    private String end;                   // "13:00"

    public WorkSlot(DayOfWeek dayOfWeek, String start, String end) {
        this.dayOfWeek = dayOfWeek;
        this.start     = start;
        this.end       = end;
    }
}
