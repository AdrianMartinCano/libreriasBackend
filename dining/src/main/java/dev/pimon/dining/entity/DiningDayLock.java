package dev.pimon.dining.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fila-cerrojo por día. Punto de sincronización: antes de comprobar qué mesas están libres y asignar
 * una reserva, se toma un bloqueo pesimista ({@code SELECT ... FOR UPDATE}) sobre la fila del día, de
 * modo que las altas del mismo día se serializan y dos grupos no pueden quedarse con la misma mesa a
 * la vez (incluida la primera reserva del día). Portable, no depende de constraints de cada BD.
 */
@Entity
@Table(name = "dining_day_locks")
@Getter
@Setter
@NoArgsConstructor
public class DiningDayLock {

    /** Día en formato ISO 'YYYY-MM-DD'. ('day' es palabra reservada → columna con nombre seguro.) */
    @Id
    @Column(name = "lock_day", length = 10)
    private String day;

    public DiningDayLock(String day) {
        this.day = day;
    }
}
