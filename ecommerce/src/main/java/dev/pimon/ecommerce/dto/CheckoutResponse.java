package dev.pimon.ecommerce.dto;

import java.math.BigDecimal;

/**
 * Respuesta del endpoint POST /api/checkout/create-intent.
 * El frontend usa clientSecret con Stripe.js para mostrar el formulario de pago.
 */
public record CheckoutResponse(
        String     orderId,
        String     clientSecret,   // Stripe PaymentIntent client secret
        BigDecimal subtotal,       // artículos, sin envío
        BigDecimal shippingCost,   // gastos de envío
        BigDecimal total,          // subtotal + envío (lo que se cobra)
        String     currency
) {}
