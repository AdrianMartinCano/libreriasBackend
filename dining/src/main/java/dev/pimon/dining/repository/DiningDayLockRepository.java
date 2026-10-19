package dev.pimon.dining.repository;

import dev.pimon.dining.entity.DiningDayLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiningDayLockRepository extends JpaRepository<DiningDayLock, String> {

    /**
     * Bloqueo pesimista de escritura sobre la fila del día (debe existir ya). Se mantiene hasta el fin
     * de la transacción → serializa las altas de ese día.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM DiningDayLock l WHERE l.day = :day")
    Optional<DiningDayLock> findAndLock(@Param("day") String day);
}
