package dev.pimon.reservations.dto;

/**
 * Franja ocupada para el calendario público. SIN datos del cliente — solo cuándo está ocupado.
 */
public record BusySlotDto(
        String date,   // YYYY-MM-DD
        String start,  // HH:mm
        String end     // HH:mm
) {}
