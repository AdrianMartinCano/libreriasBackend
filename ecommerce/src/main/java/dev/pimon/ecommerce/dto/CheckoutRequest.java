package dev.pimon.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty(message = "El carrito no puede estar vacío")
        @Valid
        List<CheckoutItemRequest> items,

        // Datos de envío opcionales — se pueden recoger después
        String shippingName,
        String shippingAddress,
        String shippingCity,
        String shippingPostalCode,
        @NotBlank(message = "El país es obligatorio")
        String shippingCountry,
        String shippingPhone
) {}
