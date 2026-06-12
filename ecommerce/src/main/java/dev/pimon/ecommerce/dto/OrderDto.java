package dev.pimon.ecommerce.dto;

import dev.pimon.ecommerce.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        String          id,
        String          userId,
        String          userEmail,
        List<OrderItemDto> items,
        double          total,
        OrderStatus     status,
        String          shippingName,
        String          shippingAddress,
        String          shippingCity,
        String          shippingCountry,
        LocalDateTime   createdAt
) {}
