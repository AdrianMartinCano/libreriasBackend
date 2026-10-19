package dev.pimon.ecommerce.web;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.ecommerce.service.OrderService;
import dev.pimon.ecommerce.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Recibe eventos de Stripe via webhook.
 * Esta ruta debe estar en public-paths (no requiere JWT).
 *
 * Configurar en Stripe Dashboard:
 *   Endpoint URL: https://tudominio.com/api/payments/webhook
 *   Eventos: payment_intent.succeeded, payment_intent.payment_failed
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeService stripeService;
    private final OrderService  orderService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        Event event = stripeService.constructWebhookEvent(payload, sigHeader);

        if (event == null) {
            // Modo simulado — ignorar
            return ResponseEntity.ok("ok (simulado)");
        }

        Optional<StripeObject> stripeObject = deserialize(event);

        switch (event.getType()) {
            case "payment_intent.succeeded" -> stripeObject.ifPresent(obj -> {
                String piId = ((PaymentIntent) obj).getId();
                log.info("Webhook: payment_intent.succeeded — {}", piId);
                orderService.handlePaymentSuccess(piId);
            });
            case "payment_intent.payment_failed" -> stripeObject.ifPresent(obj -> {
                String piId = ((PaymentIntent) obj).getId();
                log.warn("Webhook: payment_intent.payment_failed — {}", piId);
                orderService.handlePaymentFailure(piId);
            });
            default -> log.debug("Webhook evento no gestionado: {}", event.getType());
        }

        return ResponseEntity.ok("ok");
    }

    /**
     * Deserializa el objeto del evento de forma robusta.
     *
     * {@code getObject()} devuelve vacío cuando la versión de API del evento no coincide
     * con la que fija la librería stripe-java (gotcha conocido) — en ese caso el webhook
     * respondía 200 pero NO procesaba el pago, dejando el pedido en PENDING_PAYMENT.
     * Aquí caemos a {@code deserializeUnsafe()}, que ignora esa comprobación de versión.
     */
    private Optional<StripeObject> deserialize(Event event) {
        EventDataObjectDeserializer d = event.getDataObjectDeserializer();
        if (d.getObject().isPresent()) {
            return d.getObject();
        }
        try {
            return Optional.ofNullable(d.deserializeUnsafe());
        } catch (Exception e) {
            log.error("Webhook: no se pudo deserializar el evento {} — {}",
                      event.getType(), e.getMessage());
            return Optional.empty();
        }
    }
}
