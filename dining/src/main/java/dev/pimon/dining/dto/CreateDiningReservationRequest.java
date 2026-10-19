package dev.pimon.dining.dto;

import dev.pimon.dining.entity.SpecialRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Set;

/**
 * Alta de reserva. El público no elige mesa: envía fecha, turno y comensales y el backend asigna la
 * mejor mesa/combinación (best-fit). El admin puede forzar mesas concretas con {@code tableIds}.
 */
public record CreateDiningReservationRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato YYYY-MM-DD") String date,
        @NotBlank String seatingId,
        @Positive int partySize,
        @NotBlank String clientName,
        @NotBlank @Email String clientEmail,
        @NotBlank String clientPhone,
        String notes,
        Set<SpecialRequest> requests,
        /** Preferencia de zona (opcional). */
        String zoneId,
        /** Solo admin: mesas asignadas a mano. Vacío/null = asignación automática best-fit. */
        List<String> tableIds
) {}
