package dev.pimon.ecommerce.dto;

public record OrderItemDto(
        String productId,
        String productName,
        String productImageUrl,
        double unitPrice,
        int    quantity,
        double subtotal
) {}
