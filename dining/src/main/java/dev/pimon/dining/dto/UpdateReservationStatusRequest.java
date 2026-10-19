package dev.pimon.dining.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateReservationStatusRequest(@NotBlank String status) {}
