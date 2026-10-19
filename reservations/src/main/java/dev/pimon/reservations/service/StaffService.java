package dev.pimon.reservations.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.reservations.dto.SaveStaffRequest;
import dev.pimon.reservations.dto.StaffAdminDto;
import dev.pimon.reservations.dto.StaffDto;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.entity.StaffMember;
import dev.pimon.reservations.entity.WorkSlot;
import dev.pimon.reservations.repository.BookingRepository;
import dev.pimon.reservations.repository.ServiceOfferingRepository;
import dev.pimon.reservations.repository.StaffMemberRepository;
import dev.pimon.reservations.settings.TimeRange;
import dev.pimon.security.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffMemberRepository    staffRepo;
    private final ServiceOfferingRepository serviceRepo;
    private final BookingRepository        bookingRepo;

    /* ── Lecturas ── */

    /** Trabajadores activos para el flujo público de reserva. */
    @Transactional(readOnly = true)
    public List<StaffDto> listActivePublic() {
        return staffRepo.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(StaffDto::from).toList();
    }

    /** Todos (activos e inactivos) para el CRUD del panel. */
    @Transactional(readOnly = true)
    public List<StaffAdminDto> listAllAdmin() {
        return staffRepo.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(StaffAdminDto::from).toList();
    }

    public StaffMember findActiveOrThrow(String id) {
        StaffMember s = staffRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Trabajador no encontrado con id: " + id));
        if (!s.isActive()) throw AppException.conflict("El trabajador está desactivado");
        return s;
    }

    /* ── Escritura ── */

    @Transactional
    public StaffAdminDto create(SaveStaffRequest req) {
        validate(req, null);
        StaffMember s = new StaffMember();
        apply(s, req);
        return StaffAdminDto.from(staffRepo.save(s));
    }

    @Transactional
    public StaffAdminDto update(String id, SaveStaffRequest req) {
        validate(req, id);
        StaffMember s = staffRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Trabajador no encontrado con id: " + id));
        apply(s, req);
        return StaffAdminDto.from(staffRepo.save(s));
    }

    @Transactional
    public void toggleActive(String id) {
        StaffMember s = staffRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Trabajador no encontrado con id: " + id));
        s.setActive(!s.isActive());
        staffRepo.save(s);
    }

    /** Borra un trabajador. Solo si no tiene reservas: si las tiene, debe desactivarse para no perderlas. */
    @Transactional
    public void delete(String id) {
        if (!staffRepo.existsById(id)) throw AppException.notFound("Trabajador no encontrado con id: " + id);
        if (bookingRepo.existsByStaff_Id(id)) {
            throw AppException.conflict("El trabajador tiene reservas; desactívalo en vez de borrarlo");
        }
        staffRepo.deleteById(id);
    }

    /**
     * "Incluirme como profesional": crea (o devuelve) el trabajador vinculado al usuario actual.
     * Pensado para el jefe (ROLE_ADMIN) que además atiende.
     */
    @Transactional
    public StaffAdminDto createForCurrentUser(AuthUser user) {
        return staffRepo.findByUserId(user.id())
                .map(StaffAdminDto::from)
                .orElseGet(() -> {
                    StaffMember s = new StaffMember();
                    s.setName(user.email());   // editable luego en el panel
                    s.setUserId(user.id());
                    s.setActive(true);
                    return StaffAdminDto.from(staffRepo.save(s));
                });
    }

    /* ── Scoping de ROLE_STAFF ── */

    /**
     * Devuelve el {@code staffId} al que debe acotarse la consulta: para ADMIN/EDITOR es el solicitado
     * (puede ser null = todos); para un ROLE_STAFF es siempre el suyo (se ignora lo que pida).
     */
    @Transactional(readOnly = true)
    public String resolveScope(AuthUser user, String requestedStaffId) {
        if (user.isAdmin() || user.hasRole("ROLE_EDITOR")) return requestedStaffId;
        return ownStaffId(user);   // ROLE_STAFF
    }

    /** Id del trabajador vinculado al usuario, o 403 si no tiene ficha. */
    @Transactional(readOnly = true)
    public String ownStaffId(AuthUser user) {
        return staffRepo.findByUserId(user.id())
                .map(StaffMember::getId)
                .orElseThrow(() -> AppException.forbidden("Tu cuenta no está vinculada a ningún trabajador"));
    }

    /** true si el usuario es solo trabajador (no jefe ni editor) — debe verse acotado. */
    public boolean isStaffScoped(AuthUser user) {
        return !user.isAdmin() && !user.hasRole("ROLE_EDITOR");
    }

    /* ── Helpers ── */

    private void apply(StaffMember s, SaveStaffRequest req) {
        s.setName(req.name());
        s.setColor(req.color());
        s.setImageUrl(req.imageUrl());
        s.setActive(req.active());
        s.setSortOrder(req.sortOrder());
        s.setUserId(req.userId() != null && req.userId().isBlank() ? null : req.userId());
        s.setCustomHours(req.customHours());

        // weeklyHours (Map<día, tramos>) → filas WorkSlot
        s.getWorkSlots().clear();
        if (req.weeklyHours() != null) {
            for (Map.Entry<DayOfWeek, List<TimeRange>> e : req.weeklyHours().entrySet()) {
                if (e.getValue() == null) continue;
                for (TimeRange r : e.getValue()) {
                    s.getWorkSlots().add(new WorkSlot(e.getKey(), r.start(), r.end()));
                }
            }
        }

        // vacaciones
        s.getBlockedDates().clear();
        if (req.blockedDates() != null) {
            req.blockedDates().forEach(d -> s.getBlockedDates().add(LocalDate.parse(d)));
        }

        // servicios que realiza (vacío = todos)
        Set<ServiceOffering> services = new HashSet<>();
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            for (String id : req.serviceIds()) {
                services.add(serviceRepo.findById(id)
                        .orElseThrow(() -> AppException.badRequest("Servicio inexistente: " + id)));
            }
        }
        s.setServices(services);
    }

    /** Valida coherencia de tramos (fin > inicio) y unicidad del userId vinculado. */
    private void validate(SaveStaffRequest req, String currentId) {
        if (req.weeklyHours() != null) {
            req.weeklyHours().forEach((day, ranges) -> {
                if (ranges == null) return;
                for (TimeRange r : ranges) {
                    if (!LocalTime.parse(r.start()).isBefore(LocalTime.parse(r.end()))) {
                        throw AppException.badRequest("Tramo inválido en " + day +
                                ": la hora de fin debe ser posterior al inicio (" + r.start() + "–" + r.end() + ")");
                    }
                }
            });
        }
        String userId = req.userId();
        if (userId != null && !userId.isBlank()) {
            staffRepo.findByUserId(userId).ifPresent(other -> {
                if (!other.getId().equals(currentId)) {
                    throw AppException.conflict("Ese usuario ya está vinculado a otro trabajador");
                }
            });
        }
    }
}
