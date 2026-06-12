package dev.pimon.ecommerce.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.ecommerce.dto.CheckoutRequest;
import dev.pimon.ecommerce.dto.CheckoutResponse;
import dev.pimon.ecommerce.service.OrderService;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Inicia el proceso de pago.
 *
 * Flujo completo:
 *   1. POST /api/checkout/create-intent → devuelve { orderId, clientSecret }
 *   2. Frontend usa clientSecret con Stripe.js → usuario introduce tarjeta
 *   3. Stripe llama al webhook POST /api/payments/webhook
 *   4. El backend marca la orden como PAID
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<CheckoutResponse>> createIntent(
            @Valid @RequestBody CheckoutRequest request,
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(
                ApiResponse.ok(orderService.createCheckoutIntent(request, user),
                        "PaymentIntent creado. Usa el clientSecret con Stripe.js para completar el pago.")
        );
    }
}
