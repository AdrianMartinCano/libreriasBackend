package dev.pimon.reservations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fila-cerrojo por día. Sirve únicamente como punto de sincronización: se toma un bloqueo
 * pesimista ({@code SELECT ... FOR UPDATE}) sobre la fila del día antes de comprobar solapes e
 * insertar una reserva, de modo que las altas del mismo día se serializan y no pueden producirse
 * dos reservas solapadas a la vez (incluida la "primera reserva del día", que un bloqueo sobre las
 * filas existentes no cubriría).
 *
 * <p>Portable (no depende de constraints específicas de cada BD).</p>
 */
@Entity
@Table(name = "reservation_day_locks")
@Getter
@Setter
@NoArgsConstructor
public class ReservationDayLock {

    /**
     * Día en formato ISO 'YYYY-MM-DD'.
     *
     * <p>El nombre de la columna va ENTRECOMILLADO a propósito. {@code DAY} es palabra reservada
     * en H2: sin comillas, el {@code create table} fallaba en el perfil local de cada proyecto y las
     * reservas no funcionaban en local (en Postgres no pasaba). Entre comillas y en minúsculas es,
     * para Postgres, exactamente la misma columna {@code day} que ya existe en producción: no hace
     * falta migrar nada. Renombrarla (p. ej. a {@code lock_day}) sí habría roto las bases
     * existentes, porque {@code ddl-auto: update} deja la columna vieja como clave primaria.</p>
     */
    @Id
    @Column(name = "\"day\"", length = 10)
    private String day;

    public ReservationDayLock(String day) {
        this.day = day;
    }
}
