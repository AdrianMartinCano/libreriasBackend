package dev.pimon.reservations.repository;

import dev.pimon.reservations.entity.ReservationDayLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReservationDayLockRepository extends JpaRepository<ReservationDayLock, String> {

    /**
     * Toma un bloqueo pesimista de escritura sobre la fila del día (la fila debe existir ya).
     * El bloqueo se mantiene hasta el fin de la transacción → serializa las altas de ese día.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ReservationDayLock l WHERE l.day = :day")
    Optional<ReservationDayLock> findAndLock(@Param("day") String day);
}
