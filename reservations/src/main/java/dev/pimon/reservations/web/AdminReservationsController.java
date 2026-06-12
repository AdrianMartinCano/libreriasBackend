package dev.pimon.reservations.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.reservations.dto.*;
import dev.pimon.reservations.service.BookingService;
import dev.pimon.reservations.service.ServiceOfferingService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
@RequiredArgsConstructor
public class AdminReservationsController {

    private final ServiceOfferingService offeringService;
    private final BookingService         bookingService;

    /* ── Servicios ── */

    @GetMapping("/services")
    public ApiResponse<List<ServiceOfferingDto>> listAllServices() {
        return ApiResponse.ok(offeringService.listAll());
    }

    @PostMapping("/services")
    public ResponseEntity<ApiResponse<ServiceOfferingDto>> createService(
            @Valid @RequestBody CreateServiceOfferingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(offeringService.create(req), "Servicio creado"));
    }

    @PutMapping("/services/{id}")
    public ApiResponse<ServiceOfferingDto> updateService(
            @PathVariable String id,
            @Valid @RequestBody CreateServiceOfferingRequest req) {
        return ApiResponse.ok(offeringService.update(id, req));
    }

    @PatchMapping("/services/{id}/toggle-active")
    public ApiResponse<Void> toggleActive(@PathVariable String id) {
        offeringService.toggleActive(id);
        return ApiResponse.ok(null, "Visibilidad actualizada");
    }

    /* ── Reservas ── */

    @GetMapping("/bookings")
    public ApiResponse<PageResponse<BookingDto>> listBookings(
            @RequestParam(required = false) String status,
            @ParameterObject Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(bookingService.listAll(status, pageable)));
    }

    @GetMapping("/bookings/{id}")
    public ApiResponse<BookingDto> getBooking(@PathVariable String id) {
        return ApiResponse.ok(bookingService.findById(id));
    }

    @PatchMapping("/bookings/{id}/status")
    public ApiResponse<BookingDto> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookingStatusRequest req) {
        return ApiResponse.ok(bookingService.updateStatus(id, req));
    }
}
