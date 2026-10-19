package dev.pimon.reservations.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.exception.AppException;
import dev.pimon.reservations.dto.BookingDto;
import dev.pimon.reservations.dto.BusySlotDto;
import dev.pimon.reservations.dto.CreateBookingRequest;
import dev.pimon.reservations.dto.UpdateBookingStatusRequest;
import dev.pimon.reservations.entity.Booking;
import dev.pimon.reservations.entity.Booking.BookingStatus;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.entity.StaffMember;
import dev.pimon.reservations.repository.BookingRepository;
import dev.pimon.reservations.repository.ReservationDayLockRepository;
import dev.pimon.reservations.repository.StaffMemberRepository;
import dev.pimon.reservations.settings.ReservationsSettingsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository            repo;
    private final ServiceOfferingService       serviceOfferingService;
    private final AvailabilityService          availabilityService;
    private final ReservationsSettingsService  settingsService;
    private final ReservationDayLockRepository dayLockRepo;
    private final ReservationDayLockService    dayLockService;
    private final StaffMemberRepository        staffRepo;

    /**
     * Crea una reserva de forma segura ante concurrencia, asignándola a un trabajador.
     *
     * <p><b>Anti-carrera + anti-solape:</b> se toma un bloqueo pesimista sobre la fila-cerrojo del
     * día <b>antes</b> de comprobar solapes e insertar, así las altas del mismo día se serializan. El
     * solape es <b>por trabajador</b>: cada uno es una agenda independiente con su {@code capacity}
     * (recursos en paralelo; por defecto 1). Si {@code staffId} viene vacío ("cualquiera"), se asigna
     * el trabajador elegible libre menos cargado ese día.</p>
     */
    @Transactional
    public BookingDto create(CreateBookingRequest req) {
        ServiceOffering service = serviceOfferingService.findOrThrow(req.serviceId());
        LocalDate date  = LocalDate.parse(req.date());
        int duration    = req.durationMinutes() != null ? req.durationMinutes() : service.getDurationMinutes();
        LocalTime start = LocalTime.parse(req.time());
        LocalTime end   = start.plusMinutes(duration);
        int capacity    = settingsService.current().capacity();

        // Serializa las altas de este día (cubre también la primera reserva del día).
        dayLockService.ensureExists(req.date());
        dayLockRepo.findAndLock(req.date());

        StaffMember staff = resolveStaff(req, service, date, start, end, capacity);

        Booking booking = new Booking();
        booking.setService(service);
        booking.setStaff(staff);
        booking.setDate(date);
        booking.setTime(req.time());
        booking.setClientName(req.clientName());
        booking.setClientEmail(req.clientEmail());
        booking.setClientPhone(req.clientPhone());
        booking.setNotes(req.notes());
        booking.setDurationMinutes(req.durationMinutes());  // null = usa la duración del servicio

        return BookingDto.from(repo.save(booking));
    }

    /**
     * Decide a qué trabajador se asigna la cita (dentro del lock del día). Lanza 409 si el horario ya
     * no está libre. Devuelve {@code null} solo en el modo legacy "sin trabajadores configurados".
     */
    private StaffMember resolveStaff(CreateBookingRequest req, ServiceOffering service,
                                     LocalDate date, LocalTime start, LocalTime end, int capacity) {
        if (req.staffId() != null && !req.staffId().isBlank()) {
            StaffMember staff = staffRepo.findById(req.staffId()).filter(StaffMember::isActive)
                    .orElseThrow(() -> AppException.conflict("El profesional seleccionado no está disponible"));
            if (!staff.canPerform(service)) {
                throw AppException.conflict("Ese profesional no realiza este servicio");
            }
            if (!availabilityService.isWorkDayFor(staff, date)) {
                throw AppException.conflict("Ese día el profesional no trabaja");
            }
            if (overlapCountForStaff(date, staff.getId(), start, end) >= capacity) {
                throw AppException.conflict(
                        "Ese horario ya está ocupado (" + req.time() + " del " + req.date() + ")");
            }
            return staff;
        }

        // "cualquiera": elige entre los elegibles libres el menos cargado ese día (desempate sortOrder).
        List<StaffMember> candidates = availabilityService.eligibleStaff(service, date);
        if (candidates.isEmpty()) {
            if (staffRepo.count() == 0) return resolveLegacyNoStaff(req, date, start, end, capacity);
            throw AppException.conflict("Ese día no hay disponibilidad");
        }
        return candidates.stream()
                .filter(c -> overlapCountForStaff(date, c.getId(), start, end) < capacity)
                .min(Comparator
                        .comparingLong((StaffMember c) -> repo.countByStaff_IdAndDateAndStatusNot(
                                c.getId(), date, BookingStatus.CANCELLED))
                        .thenComparingInt(StaffMember::getSortOrder))
                .orElseThrow(() -> AppException.conflict(
                        "Ese horario ya está ocupado (" + req.time() + " del " + req.date() + ")"));
    }

    /** Modo compatibilidad: instalación sin trabajadores (antes del backfill). Capacidad global. */
    private StaffMember resolveLegacyNoStaff(CreateBookingRequest req, LocalDate date,
                                             LocalTime start, LocalTime end, int capacity) {
        if (!availabilityService.isWorkDay(date)) {
            throw AppException.conflict("Ese día no hay disponibilidad");
        }
        long overlapping = repo.findActiveByDateWithService(date).stream()
                .filter(b -> overlaps(b, start, end)).count();
        if (overlapping >= capacity) {
            throw AppException.conflict(
                    "Ese horario ya está ocupado (" + req.time() + " del " + req.date() + ")");
        }
        return null;
    }

    private long overlapCountForStaff(LocalDate date, String staffId, LocalTime start, LocalTime end) {
        return repo.findActiveByDateAndStaff(date, staffId).stream()
                .filter(b -> overlaps(b, start, end)).count();
    }

    /** true si la reserva existente se solapa con el intervalo [start, end). */
    private boolean overlaps(Booking b, LocalTime start, LocalTime end) {
        int dur = b.getDurationMinutes() != null ? b.getDurationMinutes() : b.getService().getDurationMinutes();
        LocalTime bStart = LocalTime.parse(b.getTime());
        LocalTime bEnd   = bStart.plusMinutes(dur);
        return start.isBefore(bEnd) && bStart.isBefore(end);   // intersección de intervalos
    }

    /**
     * Ocupación entre dos fechas para el calendario PÚBLICO. No expone datos del cliente:
     * solo fecha, hora de inicio y fin (calculado con la duración real o la del servicio).
     */
    @Transactional(readOnly = true)
    public List<BusySlotDto> busyBetween(LocalDate from, LocalDate to) {
        return repo.findBusyBetween(from, to).stream()
                .map(b -> {
                    int dur = b.getDurationMinutes() != null
                            ? b.getDurationMinutes()
                            : b.getService().getDurationMinutes();
                    return new BusySlotDto(b.getDate().toString(), b.getTime(), addMinutes(b.getTime(), dur));
                })
                .toList();
    }

    private static String addMinutes(String hhmm, int minutes) {
        return java.time.LocalTime.parse(hhmm).plusMinutes(minutes)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** Listado admin paginado, filtrable por estado y/o trabajador (ambos opcionales). */
    @Transactional(readOnly = true)
    public Page<BookingDto> listAll(String status, String staffId, Pageable pageable) {
        BookingStatus s = status != null ? parseStatus(status) : null;
        return repo.search(s, staffId, pageable).map(BookingDto::from);
    }

    /** Reservas (no canceladas) de un día, opcionalmente acotadas a un trabajador — alimenta el timeline. */
    @Transactional(readOnly = true)
    public List<BookingDto> listForDay(LocalDate date, String staffId) {
        return repo.findActiveByDateWithStaff(date).stream()
                .filter(b -> staffId == null
                        || (b.getStaff() != null && staffId.equals(b.getStaff().getId())))
                .map(BookingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingDto findById(String id) {
        return BookingDto.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<BookingDto> findByEmail(String email) {
        return repo.findByClientEmailOrderByDateDescTimeAsc(email)
                .stream().map(BookingDto::from).toList();
    }

    /** Id del trabajador de una reserva (o null) — para la comprobación de propiedad de ROLE_STAFF. */
    @Transactional(readOnly = true)
    public String staffIdOf(String bookingId) {
        StaffMember staff = findOrThrow(bookingId).getStaff();
        return staff != null ? staff.getId() : null;
    }

    @Transactional
    public BookingDto updateStatus(String id, UpdateBookingStatusRequest req) {
        Booking booking = findOrThrow(id);
        booking.setStatus(parseStatus(req.status()));
        return BookingDto.from(repo.save(booking));
    }

    private Booking findOrThrow(String id) {
        return repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Reserva no encontrada con id: " + id));
    }

    private BookingStatus parseStatus(String raw) {
        try {
            return BookingStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Estado inválido: " + raw);
        }
    }
}
