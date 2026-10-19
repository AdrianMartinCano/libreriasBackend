package dev.pimon.reservations.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.reservations.dto.TimeSlotDto;
import dev.pimon.reservations.entity.Booking;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.entity.StaffMember;
import dev.pimon.reservations.repository.BookingRepository;
import dev.pimon.reservations.repository.StaffMemberRepository;
import dev.pimon.reservations.settings.ReservationsSettings;
import dev.pimon.reservations.settings.ReservationsSettingsService;
import dev.pimon.reservations.settings.TimeRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final BookingRepository            bookingRepo;
    private final StaffMemberRepository        staffRepo;
    private final ReservationsSettingsService  settingsService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Intervalo ocupado por una reserva activa. */
    private record Interval(LocalTime start, LocalTime end) {
        boolean overlaps(LocalTime s, LocalTime e) {
            return s.isBefore(end) && start.isBefore(e);
        }
    }

    /* ───────────────────────── Entrada pública ───────────────────────── */

    /**
     * Huecos del día para un servicio. Si {@code staffId} es nulo/vacío → "cualquiera": agrega los
     * huecos de todos los trabajadores elegibles (un hueco está libre si <b>alguno</b> está libre).
     * Si viene {@code staffId} → solo los de ese trabajador.
     */
    @Transactional(readOnly = true)
    public List<TimeSlotDto> availability(LocalDate date, ServiceOffering service, String staffId) {
        if (staffId != null && !staffId.isBlank()) {
            StaffMember staff = staffRepo.findById(staffId).filter(StaffMember::isActive).orElse(null);
            if (staff == null || !staff.canPerform(service)) return List.of();
            List<Interval> booked = intervalsOf(bookingRepo.findActiveByDateAndStaff(date, staffId));
            return slotsForStaff(date, service, staff, booked);
        }
        return slotsForService(date, service);
    }

    /** Huecos agregados de todos los trabajadores elegibles ("cualquiera"). */
    @Transactional(readOnly = true)
    public List<TimeSlotDto> slotsForService(LocalDate date, ServiceOffering service) {
        List<StaffMember> workers = eligibleStaff(service, date);
        if (workers.isEmpty()) return List.of();

        // Una sola consulta del día; se agrupan las citas por trabajador en memoria (sin N+1).
        Map<String, List<Interval>> byStaff = new LinkedHashMap<>();
        for (Booking b : bookingRepo.findActiveByDateWithStaff(date)) {
            if (b.getStaff() == null) continue;
            byStaff.computeIfAbsent(b.getStaff().getId(), k -> new ArrayList<>()).add(toInterval(b));
        }

        // time → disponible (OR entre trabajadores). TreeMap para devolver ordenado por hora.
        Map<String, Boolean> merged = new TreeMap<>();
        for (StaffMember staff : workers) {
            List<Interval> booked = byStaff.getOrDefault(staff.getId(), List.of());
            for (TimeSlotDto slot : slotsForStaff(date, service, staff, booked)) {
                merged.merge(slot.time(), slot.available(), Boolean::logicalOr);
            }
        }
        return merged.entrySet().stream()
                .map(e -> TimeSlotDto.of(e.getKey(), e.getValue()))
                .toList();
    }

    /* ───────────────────────── Por trabajador ───────────────────────── */

    /**
     * Huecos de un servicio para un trabajador concreto en una fecha. Genera huecos dentro de cada
     * tramo de su horario (propio si {@code customHours}, si no el del negocio; soporta turno partido)
     * y marca disponible un hueco si reservarlo no supera la {@code capacity} por trabajador.
     */
    private List<TimeSlotDto> slotsForStaff(LocalDate date, ServiceOffering service,
                                            StaffMember staff, List<Interval> booked) {
        ReservationsSettings cfg = settingsService.current();
        List<TimeRange> ranges = rangesFor(staff, date, cfg);
        if (ranges.isEmpty()) return List.of();

        int interval = cfg.slotIntervalMinutes();
        int duration = service.getDurationMinutes();
        int capacity = cfg.capacity();   // máximo de solapes POR trabajador (por defecto 1)

        List<TimeSlotDto> slots = new ArrayList<>();
        for (TimeRange range : ranges) {
            LocalTime current   = LocalTime.parse(range.start(), TIME_FMT);
            LocalTime lastStart = LocalTime.parse(range.end(), TIME_FMT).minusMinutes(duration);
            while (!current.isAfter(lastStart)) {
                LocalTime slotStart = current;
                LocalTime slotEnd   = slotStart.plusMinutes(duration);
                long overlaps = booked.stream().filter(b -> b.overlaps(slotStart, slotEnd)).count();
                slots.add(TimeSlotDto.of(slotStart.format(TIME_FMT), overlaps < capacity));
                current = current.plusMinutes(interval);
            }
        }
        return slots;
    }

    /** Trabajadores activos que realizan el servicio y trabajan ese día. */
    @Transactional(readOnly = true)
    public List<StaffMember> eligibleStaff(ServiceOffering service, LocalDate date) {
        return staffRepo.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .filter(s -> s.canPerform(service))
                .filter(s -> isWorkDayFor(s, date))
                .toList();
    }

    /** true si el trabajador trabaja ese día (no es festivo del negocio, ni sus vacaciones, y tiene tramos). */
    public boolean isWorkDayFor(StaffMember staff, LocalDate date) {
        return !rangesFor(staff, date, settingsService.current()).isEmpty();
    }

    /**
     * Día laborable a nivel de negocio (ignora trabajadores). Solo se usa en el camino legacy "sin
     * trabajadores configurados"; con multistaff la disponibilidad va por {@link #isWorkDayFor}.
     */
    @Transactional(readOnly = true)
    public boolean isWorkDay(LocalDate date) {
        ReservationsSettings cfg = settingsService.current();
        if (cfg.blockedDates() != null && cfg.blockedDates().contains(date.toString())) return false;
        if (cfg.weeklyHours() == null) return false;
        List<TimeRange> ranges = cfg.weeklyHours().get(date.getDayOfWeek());
        return ranges != null && !ranges.isEmpty();
    }

    /**
     * Tramos abiertos del trabajador en una fecha (lista vacía = no trabaja). Aplica la regla aditiva:
     * cerrado si es festivo del negocio O vacaciones propias; horario propio si {@code customHours}.
     */
    private List<TimeRange> rangesFor(StaffMember staff, LocalDate date, ReservationsSettings cfg) {
        boolean businessClosed = cfg.blockedDates() != null && cfg.blockedDates().contains(date.toString());
        boolean staffOnHoliday = staff.getBlockedDates().contains(date);
        if (businessClosed || staffOnHoliday) return List.of();

        if (staff.isCustomHours()) {
            return staff.getWorkSlots().stream()
                    .filter(ws -> ws.getDayOfWeek() == date.getDayOfWeek())
                    .map(ws -> new TimeRange(ws.getStart(), ws.getEnd()))
                    .toList();
        }
        if (cfg.weeklyHours() == null) return List.of();
        List<TimeRange> ranges = cfg.weeklyHours().get(date.getDayOfWeek());
        return ranges != null ? ranges : List.of();
    }

    /* ───────────────────────── Helpers ───────────────────────── */

    private static List<Interval> intervalsOf(List<Booking> bookings) {
        return bookings.stream().map(AvailabilityService::toInterval).toList();
    }

    private static Interval toInterval(Booking b) {
        int dur = b.getDurationMinutes() != null ? b.getDurationMinutes() : b.getService().getDurationMinutes();
        LocalTime s = LocalTime.parse(b.getTime());
        return new Interval(s, s.plusMinutes(dur));
    }
}
