package dev.pimon.dining.dto;

import java.util.List;

/**
 * Disponibilidad de un turno para un nº de comensales en una fecha. Para el público basta
 * {@code available}; el panel admin recibe además {@code suggestedTableIds} (la mesa/combinación que
 * se asignaría) para previsualizar y poder reasignar.
 */
public record SeatingAvailabilityDto(
        String       seatingId,
        String       seatingName,
        String       start,
        String       end,
        boolean      available,
        List<String> suggestedTableIds
) {}
