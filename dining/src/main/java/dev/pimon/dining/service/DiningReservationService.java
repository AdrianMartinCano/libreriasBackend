package dev.pimon.dining.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.dining.dto.CreateDiningReservationRequest;
import dev.pimon.dining.dto.DiningReservationDto;
import dev.pimon.dining.dto.UpdateDiningReservationRequest;
import dev.pimon.dining.entity.DiningReservation;
import dev.pimon.dining.entity.DiningTable;
import dev.pimon.dining.entity.ReservationStatus;
import dev.pimon.dining.entity.Seating;
import dev.pimon.dining.repository.DiningDayLockRepository;
import dev.pimon.dining.repository.DiningReservationRepository;
import dev.pimon.dining.repository.DiningTableRepository;
import dev.pimon.dining.settings.DiningSettings;
import dev.pimon.dining.settings.DiningSettingsService;
import dev.pimon.email.dto.BookingEmailData;
import dev.pimon.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiningReservationService {

    private final DiningReservationRepository repo;
    private final DiningTableRepository       tableRepo;
    private final SeatingService              seatingService;
    private final TableAvailabilityService    availabilityService;
    private final DiningSettingsService       settingsService;
    private final DiningDayLockRepository     dayLockRepo;
    private final DiningDayLockService        dayLockService;
    /** Opcional: si el host no configura correo, no se envía nada (el email nunca rompe la reserva). */
    private final ObjectProvider<EmailService> emailService;

    /**
     * Alta de reserva segura ante concurrencia. Toma un bloqueo pesimista sobre la fila-cerrojo del
     * día <b>antes</b> de leer ocupación y asignar mesa, de modo que las altas del mismo día se
     * serializan y dos grupos no se quedan con la misma mesa. {@code adminMode}: el panel puede saltarse
     * la ventana de antelación / día cerrado y forzar mesas concretas; el público nunca.
     */
    @Transactional
    public DiningReservationDto create(CreateDiningReservationRequest req, boolean adminMode) {
        LocalDate date  = LocalDate.parse(req.date());
        Seating   seating = seatingService.findOrThrow(req.seatingId());
        DiningSettings cfg = settingsService.current();

        if (!seating.isActive()) {
            throw AppException.conflict("El turno no está disponible.");
        }
        if (!adminMode) {
            if (availabilityService.isClosed(date)) {
                throw AppException.conflict("Ese día está cerrado.");
            }
            if (!seating.appliesTo(date.getDayOfWeek())) {
                throw AppException.conflict("Ese turno no se ofrece ese día.");
            }
            validateAdvanceWindow(date, seating, cfg);
        }

        // Serializa las altas de este día (cubre también la primera del día).
        dayLockService.ensureExists(req.date());
        dayLockRepo.findAndLock(req.date());

        List<DiningReservation> active = repo.findActiveByDateWithTables(date);
        Set<DiningTable> assigned = resolveTables(req, seating, date, active, cfg, adminMode);

        DiningReservation r = new DiningReservation();
        r.setDate(date);
        r.setSeating(seating);
        r.setPartySize(req.partySize());
        r.setTables(assigned);
        r.setClientName(req.clientName());
        r.setClientEmail(req.clientEmail());
        r.setClientPhone(req.clientPhone());
        r.setNotes(req.notes());
        r.setRequests(req.requests() != null ? new LinkedHashSet<>(req.requests()) : new LinkedHashSet<>());
        r.setStatus(ReservationStatus.PENDING);
        DiningReservation saved = repo.save(r);
        sendConfirmation(saved, seating);
        return DiningReservationDto.from(saved);
    }

    /**
     * Envía la confirmación al cliente, <b>best-effort</b>: si no hay {@link EmailService} configurado
     * o el envío falla, se registra y se ignora — un email es un efecto secundario y nunca debe romper
     * la reserva ya creada.
     */
    private void sendConfirmation(DiningReservation r, Seating seating) {
        EmailService email;
        try {
            email = emailService.getIfAvailable();
        } catch (RuntimeException noMailConfigured) {
            return;
        }
        if (email == null) return;
        try {
            email.sendBookingConfirmation(BookingEmailData.builder()
                    .clientName(r.getClientName())
                    .clientEmail(r.getClientEmail())
                    .serviceName("Reserva de mesa · " + r.getPartySize() + " personas")
                    .date(r.getDate().toString())
                    .time(seating.getStart())
                    .notes(r.getNotes())
                    .status("pending")
                    .build());
        } catch (Exception e) {
            log.warn("[dining] no se pudo enviar la confirmación a {}: {}", r.getClientEmail(), e.getMessage());
        }
    }

    /** Decide qué mesas ocupa la reserva (dentro del lock). Manual solo en {@code adminMode}. */
    private Set<DiningTable> resolveTables(CreateDiningReservationRequest req, Seating seating,
                                           LocalDate date, List<DiningReservation> active,
                                           DiningSettings cfg, boolean adminMode) {
        Set<String> occupied = availabilityService.occupiedTableIds(seating, active);

        if (adminMode && req.tableIds() != null && !req.tableIds().isEmpty()) {
            List<DiningTable> tables = tableRepo.findAllById(req.tableIds());
            if (tables.size() != req.tableIds().size()) {
                throw AppException.notFound("Alguna mesa indicada no existe.");
            }
            for (DiningTable t : tables) {
                if (occupied.contains(t.getId())) {
                    throw AppException.conflict("La mesa " + t.getLabel() + " ya está ocupada en ese turno.");
                }
            }
            return new LinkedHashSet<>(tables);
        }

        // Asignación automática best-fit, respetando aforo global.
        if (!availabilityService.aforoFits(active, seating, req.partySize(), cfg.totalCapacity())) {
            throw AppException.conflict("No hay aforo para ese turno.");
        }
        List<DiningTable> free = availabilityService.freeTables(
                availabilityService.activeTables(req.zoneId()), seating, active);
        return availabilityService.findAssignment(free, req.partySize())
                .map(LinkedHashSet::new)
                .orElseThrow(() -> AppException.conflict(
                        "No hay mesa disponible para " + req.partySize() + " comensales en ese turno."));
    }

    /** Reasigna manualmente las mesas de una reserva (panel), validando que estén libres. */
    @Transactional
    public DiningReservationDto reassignTables(String id, List<String> tableIds) {
        DiningReservation r = findOrThrow(id);
        dayLockService.ensureExists(r.getDate().toString());
        dayLockRepo.findAndLock(r.getDate().toString());

        List<DiningReservation> others = repo.findActiveByDateWithTables(r.getDate()).stream()
                .filter(o -> !o.getId().equals(id))   // excluye la propia reserva
                .toList();
        Set<String> occupied = availabilityService.occupiedTableIds(r.getSeating(), others);

        List<DiningTable> tables = tableRepo.findAllById(tableIds);
        if (tables.size() != tableIds.size()) {
            throw AppException.notFound("Alguna mesa indicada no existe.");
        }
        for (DiningTable t : tables) {
            if (occupied.contains(t.getId())) {
                throw AppException.conflict("La mesa " + t.getLabel() + " ya está ocupada en ese turno.");
            }
        }
        r.setTables(new LinkedHashSet<>(tables));
        return DiningReservationDto.from(repo.save(r));
    }

    /**
     * Edición completa de una reserva (panel). Toma el lock del día destino, recalcula la ocupación
     * <b>excluyendo la propia reserva</b> y conserva las mesas actuales si siguen siendo válidas; si no
     * (cambió turno/fecha/comensales o ya no caben), reasigna best-fit, salvo {@code tableIds} manuales.
     */
    @Transactional
    public DiningReservationDto update(String id, UpdateDiningReservationRequest req) {
        DiningReservation r = findOrThrow(id);
        LocalDate newDate = LocalDate.parse(req.date());
        Seating   seating = seatingService.findOrThrow(req.seatingId());
        DiningSettings cfg = settingsService.current();
        if (!seating.isActive()) {
            throw AppException.conflict("El turno no está disponible.");
        }

        dayLockService.ensureExists(req.date());
        dayLockRepo.findAndLock(req.date());

        List<DiningReservation> others = repo.findActiveByDateWithTables(newDate).stream()
                .filter(o -> !o.getId().equals(id))   // excluye la propia reserva
                .toList();

        Set<DiningTable> tables = resolveTablesForEdit(r, req, seating, others, cfg);

        r.setDate(newDate);
        r.setSeating(seating);
        r.setPartySize(req.partySize());
        r.setTables(tables);
        r.setClientName(req.clientName());
        r.setClientEmail(req.clientEmail());
        r.setClientPhone(req.clientPhone());
        r.setNotes(req.notes());
        r.setRequests(req.requests() != null ? new LinkedHashSet<>(req.requests()) : new LinkedHashSet<>());
        if (req.status() != null && !req.status().isBlank()) {
            r.setStatus(parseStatus(req.status()));
        }
        return DiningReservationDto.from(repo.save(r));
    }

    private Set<DiningTable> resolveTablesForEdit(DiningReservation r, UpdateDiningReservationRequest req,
                                                  Seating seating, List<DiningReservation> others,
                                                  DiningSettings cfg) {
        Set<String> occupied = availabilityService.occupiedTableIds(seating, others);

        if (req.tableIds() != null && !req.tableIds().isEmpty()) {
            List<DiningTable> tables = tableRepo.findAllById(req.tableIds());
            if (tables.size() != req.tableIds().size()) {
                throw AppException.notFound("Alguna mesa indicada no existe.");
            }
            for (DiningTable t : tables) {
                if (occupied.contains(t.getId())) {
                    throw AppException.conflict("La mesa " + t.getLabel() + " ya está ocupada en ese turno.");
                }
            }
            return new LinkedHashSet<>(tables);
        }

        // Conserva las mesas actuales si siguen libres y con capacidad suficiente para el grupo.
        boolean keep = !r.getTables().isEmpty()
                && r.getTables().stream().noneMatch(t -> occupied.contains(t.getId()))
                && r.getTables().stream().mapToInt(DiningTable::getCapacityMax).sum() >= req.partySize();
        if (keep) {
            return new LinkedHashSet<>(r.getTables());
        }

        if (!availabilityService.aforoFits(others, seating, req.partySize(), cfg.totalCapacity())) {
            throw AppException.conflict("No hay aforo para ese turno.");
        }
        List<DiningTable> free = availabilityService.freeTables(
                availabilityService.activeTables(req.zoneId()), seating, others);
        return availabilityService.findAssignment(free, req.partySize())
                .map(LinkedHashSet::new)
                .orElseThrow(() -> AppException.conflict(
                        "No hay mesa disponible para " + req.partySize() + " comensales en ese turno."));
    }

    @Transactional
    public DiningReservationDto updateStatus(String id, String status) {
        DiningReservation r = findOrThrow(id);
        r.setStatus(parseStatus(status));
        return DiningReservationDto.from(repo.save(r));
    }

    /* ── Lecturas ── */

    @Transactional(readOnly = true)
    public Page<DiningReservationDto> listAll(String status, Pageable pageable) {
        ReservationStatus s = status != null ? parseStatus(status) : null;
        return repo.search(s, pageable).map(DiningReservationDto::from);
    }

    /** Reservas de un día (cualquier estado) — alimenta el plano de sala del admin. */
    @Transactional(readOnly = true)
    public List<DiningReservationDto> listForDay(LocalDate date) {
        return repo.findByDateWithTables(date).stream().map(DiningReservationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DiningReservationDto findById(String id) {
        return DiningReservationDto.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<DiningReservationDto> findByEmail(String email) {
        return repo.findByClientEmailOrderByDateDesc(email).stream()
                .map(DiningReservationDto::from).toList();
    }

    /* ── Internos ── */

    private void validateAdvanceWindow(LocalDate date, Seating seating, DiningSettings cfg) {
        LocalDateTime start = LocalDateTime.of(date, LocalTime.parse(seating.getStart()));
        LocalDateTime now   = LocalDateTime.now();
        if (start.isBefore(now.plusHours(cfg.minAdvanceHours()))) {
            throw AppException.conflict("Demasiado tarde para reservar ese turno.");
        }
        if (date.isAfter(LocalDate.now().plusDays(cfg.maxAdvanceDays()))) {
            throw AppException.conflict("Aún no se admiten reservas tan lejanas.");
        }
    }

    private DiningReservation findOrThrow(String id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Reserva no encontrada: " + id));
    }

    private ReservationStatus parseStatus(String raw) {
        try {
            return ReservationStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Estado inválido: " + raw);
        }
    }
}
