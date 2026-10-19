package dev.pimon.dining.service;

import dev.pimon.dining.entity.DiningDayLock;
import dev.pimon.dining.repository.DiningDayLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantiza la existencia de la fila-cerrojo de un día.
 *
 * <p>Transacción <b>propia</b> ({@code REQUIRES_NEW}): si dos peticiones intentan crear la fila a la
 * vez, el choque de clave de una aborta solo esta transacción interna y <b>no contamina</b> la de la
 * reserva. Es idempotente. Bean separado a propósito: {@code REQUIRES_NEW} solo surte efecto a través
 * del proxy de Spring (no en una auto-invocación dentro de {@code DiningReservationService}).</p>
 */
@Service
@RequiredArgsConstructor
public class DiningDayLockService {

    private final DiningDayLockRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(String day) {
        if (repo.existsById(day)) return;
        try {
            repo.saveAndFlush(new DiningDayLock(day));
        } catch (DataIntegrityViolationException concurrentInsert) {
            // Otra petición la creó a la vez — perfecto, ya existe.
        }
    }
}
