package dev.pimon.events.dto;

import java.util.Map;

/** Resumen agregado para el organizador (sin datos personales). */
public record RsvpSummaryDto(
        String eventId,
        long confirmed,
        long declined,
        long totalGuests,
        Map<String, Long> byMenu
) {}
