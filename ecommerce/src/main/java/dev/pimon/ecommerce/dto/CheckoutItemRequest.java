package dev.pimon.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CheckoutItemRequest(
        @NotBlank String productId,
        @Positive int    quantity
) {}
