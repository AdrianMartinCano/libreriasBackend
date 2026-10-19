package dev.pimon.dining.entity;

/**
 * Ciclo de vida de una reserva de mesa. {@code PENDING/CONFIRMED/SEATED} cuentan como activas (ocupan
 * mesa y aforo); {@code NO_SHOW/CANCELLED/COMPLETED} liberan.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    SEATED,
    NO_SHOW,
    CANCELLED,
    COMPLETED
}
