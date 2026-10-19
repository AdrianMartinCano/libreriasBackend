package dev.pimon.menu.dto;

import dev.pimon.menu.entity.Allergen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Alta/edición de plato. Si {@code portions} viene vacío, manda {@code price}; si trae raciones,
 * {@code price} actúa como "desde" (puede ser null). La coherencia la valida el servicio.
 */
public record CreateMenuItemRequest(
        @NotBlank String name,
        String description,
        @PositiveOrZero BigDecimal price,
        @Valid List<PortionRequest> portions,
        Set<Allergen> allergens,
        boolean vegan,
        boolean vegetarian,
        boolean glutenFree,
        boolean spicy,
        String badge,
        @PositiveOrZero int sortOrder,
        @NotBlank String categoryId
) {
    /** Una ración con precio dentro del alta del plato. */
    public record PortionRequest(
            @NotBlank String label,
            @PositiveOrZero BigDecimal amount
    ) {}
}
