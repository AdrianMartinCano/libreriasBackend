package dev.pimon.events.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.events.dto.RsvpDto;
import dev.pimon.events.dto.RsvpRequest;
import dev.pimon.events.dto.RsvpSummaryDto;
import dev.pimon.events.service.RsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del RSVP consumidos por la librería Angular {@code @org/events}.
 * El resumen expone solo cifras agregadas (sin datos personales), por eso es público.
 */
@RestController
@RequestMapping("/api/events/rsvp")
@RequiredArgsConstructor
public class PublicEventsController {

    private final RsvpService service;

    /** Confirma la asistencia de un invitado. */
    @PostMapping
    public ApiResponse<RsvpDto> submit(@Valid @RequestBody RsvpRequest req) {
        return ApiResponse.ok(service.create(req), "¡Gracias por confirmar!");
    }

    /** Resumen agregado para el organizador (confirmados, ausencias, asistentes, reparto por menú). */
    @GetMapping("/summary")
    public ApiResponse<RsvpSummaryDto> summary(@RequestParam String eventId) {
        return ApiResponse.ok(service.summary(eventId));
    }
}
