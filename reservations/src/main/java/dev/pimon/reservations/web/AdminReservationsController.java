package dev.pimon.reservations.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.reservations.dto.*;
import dev.pimon.reservations.service.BookingService;
import dev.pimon.reservations.service.ServiceOfferingService;
import dev.pimon.reservations.service.StaffService;
import dev.pimon.reservations.settings.ReservationsSettings;
import dev.pimon.reservations.settings.ReservationsSettingsService;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'STAFF')")
@RequiredArgsConstructor
public class AdminReservationsController {

    private final ServiceOfferingService        offeringService;
    private final BookingService                bookingService;
    private final ReservationsSettingsService   settingsService;
    private final StaffService                  staffService;

    /* ── Ajustes (días/horario/intervalo/capacidad) ── */

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<ReservationsSettings> getSettings() {
        return ApiResponse.ok(settingsService.current());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<ReservationsSettings> updateSettings(
            @Valid @RequestBody ReservationsSettings settings) {
        return ApiResponse.ok(settingsService.update(settings), "Ajustes guardados");
    }

    /* ── Servicios ── */

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<List<ServiceOfferingDto>> listAllServices() {
        return ApiResponse.ok(offeringService.listAll());
    }

    @PostMapping("/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<ServiceOfferingDto>> createService(
            @Valid @RequestBody CreateServiceOfferingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(offeringService.create(req), "Servicio creado"));
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<ServiceOfferingDto> updateService(
            @PathVariable String id,
            @Valid @RequestBody CreateServiceOfferingRequest req) {
        return ApiResponse.ok(offeringService.update(id, req));
    }

    @PatchMapping("/services/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<Void> toggleActive(@PathVariable String id) {
        offeringService.toggleActive(id);
        return ApiResponse.ok(null, "Visibilidad actualizada");
    }

    /* ── Profesionales (solo el jefe/editor gestiona el equipo) ── */

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<List<StaffAdminDto>> listStaff() {
        return ApiResponse.ok(staffService.listAllAdmin());
    }

    @PostMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<StaffAdminDto>> createStaff(@Valid @RequestBody SaveStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(staffService.create(req), "Profesional creado"));
    }

    @PutMapping("/staff/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<StaffAdminDto> updateStaff(
            @PathVariable String id, @Valid @RequestBody SaveStaffRequest req) {
        return ApiResponse.ok(staffService.update(id, req));
    }

    @PatchMapping("/staff/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<Void> toggleStaffActive(@PathVariable String id) {
        staffService.toggleActive(id);
        return ApiResponse.ok(null, "Estado del profesional actualizado");
    }

    @DeleteMapping("/staff/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<Void> deleteStaff(@PathVariable String id) {
        staffService.delete(id);
        return ApiResponse.ok(null, "Profesional eliminado");
    }

    /** Atajo "incluirme como profesional": crea (o devuelve) la ficha vinculada al usuario actual. */
    @PostMapping("/staff/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ApiResponse<StaffAdminDto> createMyStaff(@CurrentUser AuthUser user) {
        return ApiResponse.ok(staffService.createForCurrentUser(user), "Ya apareces como profesional");
    }

    /* ── Reservas ── */

    @GetMapping("/bookings")
    public ApiResponse<PageResponse<BookingDto>> listBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String staffId,
            @CurrentUser AuthUser user,
            @ParameterObject Pageable pageable) {
        String scoped = staffService.resolveScope(user, staffId);   // ROLE_STAFF queda acotado a lo suyo
        return ApiResponse.ok(PageResponse.from(bookingService.listAll(status, scoped, pageable)));
    }

    /** Reservas de un día (para el timeline). El jefe ve todas/columna filtrada; el trabajador, solo la suya. */
    @GetMapping("/bookings/day")
    public ApiResponse<List<BookingDto>> bookingsForDay(
            @RequestParam String date,
            @RequestParam(required = false) String staffId,
            @CurrentUser AuthUser user) {
        String scoped = staffService.resolveScope(user, staffId);
        return ApiResponse.ok(bookingService.listForDay(LocalDate.parse(date), scoped));
    }

    @GetMapping("/bookings/{id}")
    public ApiResponse<BookingDto> getBooking(@PathVariable String id, @CurrentUser AuthUser user) {
        assertCanManage(user, id);
        return ApiResponse.ok(bookingService.findById(id));
    }

    @PatchMapping("/bookings/{id}/status")
    public ApiResponse<BookingDto> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookingStatusRequest req,
            @CurrentUser AuthUser user) {
        assertCanManage(user, id);
        return ApiResponse.ok(bookingService.updateStatus(id, req));
    }

    /** Un ROLE_STAFF solo puede tocar reservas de su propia agenda; ADMIN/EDITOR, cualquiera. */
    private void assertCanManage(AuthUser user, String bookingId) {
        if (!staffService.isStaffScoped(user)) return;
        String own = staffService.ownStaffId(user);
        if (!own.equals(bookingService.staffIdOf(bookingId))) {
            throw AppException.forbidden("No puedes gestionar reservas de otro profesional");
        }
    }
}
