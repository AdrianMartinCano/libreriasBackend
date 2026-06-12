package dev.pimon.ecommerce.dto;

/**
 * Respuesta del endpoint POST /api/checkout/create-intent.
 * El frontend usa clientSecret con Stripe.js para mostrar el formulario de pago.
 */
public record CheckoutResponse(
        String orderId,
        String clientSecret,   // Stripe PaymentIntent client secret
        double total,
        String currency
) {}
