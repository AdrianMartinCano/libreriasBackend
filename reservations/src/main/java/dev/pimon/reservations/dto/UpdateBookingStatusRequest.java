package dev.pimon.reservations.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBookingStatusRequest(
        @NotBlank String status   // "confirmed", "cancelled", "completed"
) {}
