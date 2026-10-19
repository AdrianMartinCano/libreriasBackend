package dev.pimon.reservations.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.reservations.dto.*;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.service.AvailabilityService;
import dev.pimon.reservations.service.BookingService;
import dev.pimon.reservations.service.ServiceOfferingService;
import dev.pimon.reservations.service.StaffService;
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
    private final StaffService           staffService;

    /** Lista los servicios activos disponibles para reservar. */
    @GetMapping("/services")
    public ApiResponse<List<ServiceOfferingDto>> listServices() {
        return ApiResponse.ok(offeringService.listActive());
    }

    /** Lista los trabajadores activos para que el cliente elija profesional (o "cualquiera"). */
    @GetMapping("/staff")
    public ApiResponse<List<StaffDto>> listStaff() {
        return ApiResponse.ok(staffService.listActivePublic());
    }

    /**
     * Devuelve los slots disponibles para un servicio en una fecha. {@code staffId} opcional: si se
     * omite, agrega la disponibilidad de todos los profesionales ("cualquiera").
     * Ejemplo: GET /api/availability?serviceId=uuid&date=2025-06-15&staffId=uuid
     */
    @GetMapping("/availability")
    public ApiResponse<List<TimeSlotDto>> availability(
            @RequestParam String serviceId,
            @RequestParam String date,
            @RequestParam(required = false) String staffId) {

        ServiceOffering service = offeringService.findOrThrow(serviceId);
        LocalDate localDate = LocalDate.parse(date);
        return ApiResponse.ok(availabilityService.availability(localDate, service, staffId));
    }

    /** Crea una nueva reserva (público — el cliente no necesita estar autenticado). */
    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(
            @Valid @RequestBody CreateBookingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(bookingService.create(req), "Reserva creada correctamente"));
    }

    /**
     * Calendario público de ocupación entre dos fechas — SIN datos del cliente.
     * Ejemplo: GET /api/busy?from=2025-06-01&to=2025-06-30
     */
    @GetMapping("/busy")
    public ApiResponse<List<BusySlotDto>> busy(
            @RequestParam String from,
            @RequestParam String to) {
        return ApiResponse.ok(bookingService.busyBetween(LocalDate.parse(from), LocalDate.parse(to)));
    }
}
