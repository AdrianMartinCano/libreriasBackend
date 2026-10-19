package dev.pimon.dining.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Turno de servicio (seating): "Comida" 13:00–15:00, "Cena 1er turno" 20:00–22:00, etc. La reserva
 * ocupa la mesa {@code durationMinutes} a partir de la hora de inicio; dos turnos del mismo día se
 * consideran ocupados si sus ventanas se solapan.
 */
@Entity
@Table(name = "dining_seatings")
@Getter
@Setter
@NoArgsConstructor
public class Seating extends BaseEntity {

    @Column(nullable = false)
    private String name;            // "Comida", "Cena"

    // 'start'/'end' son palabras reservadas en SQL → se mapean a columnas con nombre seguro.
    @Column(name = "start_time", nullable = false, length = 5)
    private String start;           // "20:00"

    @Column(name = "end_time", nullable = false, length = 5)
    private String end;             // "22:00"

    /** Cuánto ocupa la mesa la reserva de este turno (puede ser < ventana del turno). */
    @Column(nullable = false)
    private int durationMinutes;

    /** Días de la semana en los que aplica el turno. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dining_seating_days", joinColumns = @JoinColumn(name = "seating_id"))
    @Column(name = "day_of_week", length = 10)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> daysOfWeek = new LinkedHashSet<>();

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    public boolean appliesTo(DayOfWeek day) {
        return active && daysOfWeek.contains(day);
    }
}
