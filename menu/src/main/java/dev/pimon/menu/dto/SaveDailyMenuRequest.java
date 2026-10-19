package dev.pimon.menu.dto;

import dev.pimon.menu.entity.Allergen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Alta/edición de un menú del día con sus grupos y opciones. Una opción referencia un plato de la
 * carta ({@code itemId}) o es una línea libre ({@code name}); la validación cruzada la hace el servicio.
 */
public record SaveDailyMenuRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato YYYY-MM-DD") String date,
        @NotBlank String name,
        @PositiveOrZero BigDecimal price,
        String description,
        @Valid List<CourseRequest> courses
) {
    public record CourseRequest(
            @NotBlank String name,
            @Positive int choose,
            @Valid List<OptionRequest> options
    ) {}

    public record OptionRequest(
            String itemId,        // referencia a la carta (o null si es línea libre)
            String name,          // usado si itemId == null
            String description,
            @PositiveOrZero BigDecimal supplement,
            Set<Allergen> allergens
    ) {}
}
