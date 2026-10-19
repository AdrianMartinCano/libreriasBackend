package dev.pimon.dining.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.dining.dto.*;
import dev.pimon.dining.service.DiningReservationService;
import dev.pimon.dining.service.DiningTableService;
import dev.pimon.dining.service.SeatingService;
import dev.pimon.dining.service.TableAvailabilityService;
import dev.pimon.dining.service.ZoneService;
import dev.pimon.dining.settings.DiningSettings;
import dev.pimon.dining.settings.DiningSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel de administración de reservas de restaurante: ajustes, zonas, mesas, turnos y reservas
 * (incluido el plano del día y la asignación manual de mesas).
 */
@RestController
@RequestMapping("/api/admin/dining")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
@RequiredArgsConstructor
public class AdminDiningController {

    private final DiningSettingsService    settingsService;
    private final ZoneService              zoneService;
    private final DiningTableService       tableService;
    private final SeatingService           seatingService;
    private final DiningReservationService reservationService;
    private final TableAvailabilityService availabilityService;

    /* ── Ajustes ── */

    @GetMapping("/settings")
    public ApiResponse<DiningSettings> getSettings() {
        return ApiResponse.ok(settingsService.current());
    }

    @PutMapping("/settings")
    public ApiResponse<DiningSettings> updateSettings(@Valid @RequestBody DiningSettings settings) {
        return ApiResponse.ok(settingsService.update(settings), "Ajustes guardados");
    }

    /* ── Zonas ── */

    @GetMapping("/zones")
    public ApiResponse<List<ZoneDto>> listZones() {
        return ApiResponse.ok(zoneService.listAll());
    }

    @PostMapping("/zones")
    public ResponseEntity<ApiResponse<ZoneDto>> createZone(@Valid @RequestBody SaveZoneRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(zoneService.create(req), "Zona creada"));
    }

    @PutMapping("/zones/{id}")
    public ApiResponse<ZoneDto> updateZone(@PathVariable String id, @Valid @RequestBody SaveZoneRequest req) {
        return ApiResponse.ok(zoneService.update(id, req));
    }

    @PatchMapping("/zones/{id}/toggle-active")
    public ApiResponse<Void> toggleZone(@PathVariable String id) {
        zoneService.toggleActive(id);
        return ApiResponse.ok(null, "Zona actualizada");
    }

    @DeleteMapping("/zones/{id}")
    public ApiResponse<Void> deleteZone(@PathVariable String id) {
        zoneService.delete(id);
        return ApiResponse.ok(null, "Zona eliminada");
    }

    /* ── Mesas ── */

    @GetMapping("/tables")
    public ApiResponse<List<DiningTableDto>> listTables() {
        return ApiResponse.ok(tableService.listAll());
    }

    @PostMapping("/tables")
    public ResponseEntity<ApiResponse<DiningTableDto>> createTable(@Valid @RequestBody SaveTableRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tableService.create(req), "Mesa creada"));
    }

    @PutMapping("/tables/{id}")
    public ApiResponse<DiningTableDto> updateTable(@PathVariable String id, @Valid @RequestBody SaveTableRequest req) {
        return ApiResponse.ok(tableService.update(id, req));
    }

    @PatchMapping("/tables/{id}/toggle-active")
    public ApiResponse<Void> toggleTable(@PathVariable String id) {
        tableService.toggleActive(id);
        return ApiResponse.ok(null, "Mesa actualizada");
    }

    @DeleteMapping("/tables/{id}")
    public ApiResponse<Void> deleteTable(@PathVariable String id) {
        tableService.delete(id);
        return ApiResponse.ok(null, "Mesa eliminada");
    }

    /* ── Turnos ── */

    @GetMapping("/seatings")
    public ApiResponse<List<SeatingDto>> listSeatings() {
        return ApiResponse.ok(seatingService.listAll());
    }

    @PostMapping("/seatings")
    public ResponseEntity<ApiResponse<SeatingDto>> createSeating(@Valid @RequestBody SaveSeatingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(seatingService.create(req), "Turno creado"));
    }

    @PutMapping("/seatings/{id}")
    public ApiResponse<SeatingDto> updateSeating(@PathVariable String id, @Valid @RequestBody SaveSeatingRequest req) {
        return ApiResponse.ok(seatingService.update(id, req));
    }

    @PatchMapping("/seatings/{id}/toggle-active")
    public ApiResponse<Void> toggleSeating(@PathVariable String id) {
        seatingService.toggleActive(id);
        return ApiResponse.ok(null, "Turno actualizado");
    }

    @DeleteMapping("/seatings/{id}")
    public ApiResponse<Void> deleteSeating(@PathVariable String id) {
        seatingService.delete(id);
        return ApiResponse.ok(null, "Turno eliminado");
    }

    /* ── Reservas ── */

    /** Disponibilidad por turno con los ids de mesa sugeridos (para previsualizar la asignación). */
    @GetMapping("/availability")
    public ApiResponse<List<SeatingAvailabilityDto>> availability(
            @RequestParam String date,
            @RequestParam int party,
            @RequestParam(required = false) String zoneId) {
        return ApiResponse.ok(availabilityService.availability(LocalDate.parse(date), party, zoneId));
    }

    @GetMapping("/reservations")
    public ApiResponse<PageResponse<DiningReservationDto>> listReservations(
            @RequestParam(required = false) String status,
            @ParameterObject Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(reservationService.listAll(status, pageable)));
    }

    /** Reservas de un día — alimenta el plano de sala. */
    @GetMapping("/reservations/day")
    public ApiResponse<List<DiningReservationDto>> reservationsForDay(@RequestParam String date) {
        return ApiResponse.ok(reservationService.listForDay(LocalDate.parse(date)));
    }

    @GetMapping("/reservations/{id}")
    public ApiResponse<DiningReservationDto> getReservation(@PathVariable String id) {
        return ApiResponse.ok(reservationService.findById(id));
    }

    /** Alta desde el panel (modo admin: puede saltarse antelación/cierre y forzar mesas con tableIds). */
    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<DiningReservationDto>> createReservation(
            @Valid @RequestBody CreateDiningReservationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reservationService.create(req, true), "Reserva creada"));
    }

    /** Edición completa de la reserva (fecha, turno, comensales, cliente, estado y mesas). */
    @PutMapping("/reservations/{id}")
    public ApiResponse<DiningReservationDto> updateReservation(
            @PathVariable String id, @Valid @RequestBody UpdateDiningReservationRequest req) {
        return ApiResponse.ok(reservationService.update(id, req), "Reserva actualizada");
    }

    @PatchMapping("/reservations/{id}/status")
    public ApiResponse<DiningReservationDto> updateStatus(
            @PathVariable String id, @Valid @RequestBody UpdateReservationStatusRequest req) {
        return ApiResponse.ok(reservationService.updateStatus(id, req.status()));
    }

    /** Reasigna manualmente las mesas de una reserva (drag&drop del plano). */
    @PatchMapping("/reservations/{id}/tables")
    public ApiResponse<DiningReservationDto> reassignTables(
            @PathVariable String id, @Valid @RequestBody ReassignTablesRequest req) {
        return ApiResponse.ok(reservationService.reassignTables(id, req.tableIds()));
    }
}
