package dev.pimon.reservations.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.reservations.dto.*;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.service.AvailabilityService;
import dev.pimon.reservations.service.BookingService;
import dev.pimon.reservations.service.ServiceOfferingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicReservationsController {

    private final ServiceOfferingService offeringService;
    private final AvailabilityService    availabilityService;
    private final BookingService         bookingService;

    /** Lista los servicios activos disponibles para reservar. */
    @GetMapping("/services")
    public ApiResponse<List<ServiceOfferingDto>> listServices() {
        return ApiResponse.ok(offeringService.listActive());
    }

    /**
     * Devuelve los slots disponibles para un servicio en una fecha.
     * Ejemplo: GET /api/availability?serviceId=uuid&date=2025-06-15
     */
    @GetMapping("/availability")
    public ApiResponse<List<TimeSlotDto>> availability(
            @RequestParam String serviceId,
            @RequestParam String date) {

        ServiceOffering service = offeringService.findOrThrow(serviceId);
        LocalDate localDate = LocalDate.parse(date);
        return ApiResponse.ok(availabilityService.slotsForDate(localDate, service));
    }

    /** Crea una nueva reserva (público — el cliente no necesita estar autenticado). */
    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(
            @Valid @RequestBody CreateBookingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(bookingService.create(req), "Reserva creada correctamente"));
    }
}
