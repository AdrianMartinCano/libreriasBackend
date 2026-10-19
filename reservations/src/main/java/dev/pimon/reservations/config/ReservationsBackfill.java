package dev.pimon.reservations.config;

import dev.pimon.reservations.entity.StaffMember;
import dev.pimon.reservations.repository.BookingRepository;
import dev.pimon.reservations.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migración del modo single-staff a multistaff. Idempotente: solo actúa si <b>no hay ningún
 * trabajador</b>. Crea un trabajador por defecto y le asigna todas las reservas previas (que tenían
 * {@code staff_id} nulo), de modo que una instalación antigua sigue funcionando sin tocar datos a mano.
 *
 * <p>Se ejecuta al arrancar; en arranques posteriores no hace nada porque ya existe el trabajador.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationsBackfill {

    private final StaffMemberRepository staffRepo;
    private final BookingRepository     bookingRepo;
    private final ReservationsProperties props;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultStaffIfEmpty() {
        if (staffRepo.count() > 0) return;

        StaffMember staff = new StaffMember();
        staff.setName(props.getDefaultStaffName());
        staff.setActive(true);
        staff = staffRepo.save(staff);

        int reassigned = bookingRepo.assignNullStaffTo(staff);
        log.info("Multistaff: creado trabajador por defecto '{}' y asignadas {} reservas previas.",
                staff.getName(), reassigned);
    }
}
