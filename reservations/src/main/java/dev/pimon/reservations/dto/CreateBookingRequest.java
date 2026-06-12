package dev.pimon.reservations.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBookingRequest(
        @NotBlank String serviceId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato: YYYY-MM-DD") String date,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "Formato: HH:mm") String time,
        @NotBlank String clientName,
        @NotBlank @Email String clientEmail,
        @NotBlank String clientPhone,
        String notes
) {}
