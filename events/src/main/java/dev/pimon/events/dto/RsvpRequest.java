package dev.pimon.events.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Payload de confirmación de asistencia que envía el formulario RSVP del front. */
public record RsvpRequest(
        @NotBlank String eventId,
        @NotBlank String name,
        @Email String email,
        @NotNull Boolean attending,
        @PositiveOrZero Integer guests,
        String menuChoice,
        String allergies,
        String song,
        String message
) {}
