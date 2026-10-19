package dev.pimon.dining.dto;

import dev.pimon.dining.entity.SpecialRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Set;

/**
 * Edición completa de una reserva desde el panel. Puede cambiar fecha, turno, comensales, datos del
 * cliente y estado. Las mesas se conservan si siguen siendo válidas; si se cambia algo que lo
 * requiere (o no caben), se reasignan best-fit, salvo que se indiquen {@code tableIds} a mano.
 */
public record UpdateDiningReservationRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato YYYY-MM-DD") String date,
        @NotBlank String seatingId,
        @Positive int partySize,
        @NotBlank String clientName,
        @NotBlank @Email String clientEmail,
        @NotBlank String clientPhone,
        String notes,
        Set<SpecialRequest> requests,
        /** Nuevo estado (opcional; si se omite, no cambia). */
        String status,
        /** Preferencia de zona para la reasignación automática (opcional). */
        String zoneId,
        /** Mesas forzadas a mano (opcional). Vacío = conservar o reasignar automáticamente. */
        List<String> tableIds
) {}
