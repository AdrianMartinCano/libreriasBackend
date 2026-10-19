package dev.pimon.dining.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.dining.dto.CreateDiningReservationRequest;
import dev.pimon.dining.dto.DiningReservationDto;
import dev.pimon.dining.dto.SeatingAvailabilityDto;
import dev.pimon.dining.dto.SeatingDto;
import dev.pimon.dining.dto.ZoneDto;
import dev.pimon.dining.service.DiningReservationService;
import dev.pimon.dining.service.SeatingService;
import dev.pimon.dining.service.TableAvailabilityService;
import dev.pimon.dining.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API pública de reservas de restaurante (sin autenticación). El cliente no elige mesa: indica
 * fecha, turno y comensales y el backend asigna la mejor mesa.
 */
@RestController
@RequestMapping("/api/dining")
@RequiredArgsConstructor
public class PublicDiningController {

    private final ZoneService              zoneService;
    private final SeatingService           seatingService;
    private final TableAvailabilityService availabilityService;
    private final DiningReservationService reservationService;

    /** Zonas activas (para elegir preferencia: terraza/salón...). */
    @GetMapping("/zones")
    public ApiResponse<List<ZoneDto>> zones() {
        return ApiResponse.ok(zoneService.listActive());
    }

    /** Turnos activos. */
    @GetMapping("/seatings")
    public ApiResponse<List<SeatingDto>> seatings() {
        return ApiResponse.ok(seatingService.listActive());
    }

    /**
     * Disponibilidad por turno para una fecha y nº de comensales. No expone qué mesas se asignarían.
     * Ej.: GET /api/dining/availability?date=2026-06-20&party=4&zoneId=uuid
     */
    @GetMapping("/availability")
    public ApiResponse<List<SeatingAvailabilityDto>> availability(
            @RequestParam String date,
            @RequestParam int party,
            @RequestParam(required = false) String zoneId) {
        // El público recibe solo si hay hueco, sin los ids de mesa sugeridos.
        List<SeatingAvailabilityDto> result = availabilityService.availability(LocalDate.parse(date), party, zoneId)
                .stream()
                .map(a -> new SeatingAvailabilityDto(
                        a.seatingId(), a.seatingName(), a.start(), a.end(), a.available(), List.of()))
                .toList();
        return ApiResponse.ok(result);
    }

    /** Crea una reserva (público — modo no admin: respeta antelación/días cerrados). */
    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<DiningReservationDto>> create(
            @Valid @RequestBody CreateDiningReservationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reservationService.create(req, false), "Reserva creada correctamente"));
    }
}
