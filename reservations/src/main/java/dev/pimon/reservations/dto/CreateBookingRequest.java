package dev.pimon.reservations.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateBookingRequest(
        @NotBlank String serviceId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato: YYYY-MM-DD") String date,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "Formato: HH:mm") String time,
        @NotBlank String clientName,
        @NotBlank @Email String clientEmail,
        @NotBlank String clientPhone,
        String notes,
        /** Duración personalizada en minutos (alta admin de X horas). Si es null, se usa la del servicio. */
        @Positive Integer durationMinutes,
        /** Trabajador concreto solicitado. null/vacío = "cualquiera": el servidor asigna el primero libre. */
        String staffId
) {}
