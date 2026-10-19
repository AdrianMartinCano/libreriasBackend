package dev.pimon.reservations.service;

import dev.pimon.reservations.entity.ReservationDayLock;
import dev.pimon.reservations.repository.ReservationDayLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantiza la existencia de la fila-cerrojo de un día.
 *
 * <p>Se ejecuta en una transacción <b>propia</b> ({@code REQUIRES_NEW}) para que, si dos peticiones
 * intentan crear la fila a la vez, el choque de clave primaria de una de ellas aborte solo esta
 * transacción interna y <b>no contamine</b> la transacción de la reserva. Es idempotente.</p>
 *
 * <p>Bean separado a propósito: {@code REQUIRES_NEW} solo surte efecto a través del proxy de Spring
 * (no en una auto-invocación dentro de {@code BookingService}).</p>
 */
@Service
@RequiredArgsConstructor
public class ReservationDayLockService {

    private final ReservationDayLockRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(String day) {
        if (repo.existsById(day)) return;
        try {
            repo.saveAndFlush(new ReservationDayLock(day));
        } catch (DataIntegrityViolationException concurrentInsert) {
            // Otra petición la creó a la vez — perfecto, ya existe.
        }
    }
}
