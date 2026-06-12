package dev.pimon.ecommerce.entity;

public enum OrderStatus {
    PENDING_PAYMENT,  // creada, esperando pago
    PAID,             // Stripe confirmó el pago
    PROCESSING,       // preparando el envío
    SHIPPED,          // enviado
    DELIVERED,        // entregado
    CANCELLED,        // cancelado
    REFUNDED          // devuelto
}
