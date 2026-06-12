package dev.pimon.ecommerce.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.exception.AppException;
import dev.pimon.ecommerce.config.EcommerceProperties;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servicio de pagos con Stripe.
 *
 * Si libui.ecommerce.stripe.enabled=false (o no hay secret-key),
 * opera en modo simulado: crea un clientSecret falso para poder
 * desarrollar sin una cuenta de Stripe real.
 *
 * En producción:
 *   1. libui.ecommerce.stripe.enabled=true
 *   2. libui.ecommerce.stripe.secret-key=sk_live_...
 *   3. Configurar el webhook en Stripe Dashboard → /api/payments/webhook
 *   4. libui.ecommerce.stripe.webhook-secret=whsec_...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final EcommerceProperties props;

    /**
     * Crea un PaymentIntent en Stripe y devuelve el clientSecret.
     * El frontend usa este clientSecret con Stripe.js para mostrar el formulario.
     *
     * @param amountInCents importe en céntimos (ej. 2990 = 29,90 €)
     * @param orderId       para incluirlo como metadata en Stripe
     * @return clientSecret del PaymentIntent
     */
    public String createPaymentIntent(long amountInCents, String orderId) {
        if (!isEnabled()) {
            log.warn("[Stripe SIMULADO] PaymentIntent para orden {} — {}€",
                     orderId, amountInCents / 100.0);
            return "pi_demo_" + UUID.randomUUID() + "_secret_demo";
        }

        Stripe.apiKey = props.getStripe().getSecretKey();
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(props.getCurrency())
                    .putMetadata("orderId", orderId)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            log.info("PaymentIntent creado: {} para orden {}", intent.getId(), orderId);
            return intent.getClientSecret();

        } catch (StripeException e) {
            log.error("Error creando PaymentIntent: {}", e.getMessage());
            throw AppException.internalError("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * Extrae el id del PaymentIntent del clientSecret.
     * Formato: pi_xxxxx_secret_yyyyy → pi_xxxxx
     */
    public String extractPaymentIntentId(String clientSecret) {
        if (clientSecret == null) return null;
        int idx = clientSecret.indexOf("_secret_");
        return idx > 0 ? clientSecret.substring(0, idx) : clientSecret;
    }

    /**
     * Valida y parsea el evento del webhook de Stripe.
     * Lanza excepción si la firma no es válida.
     */
    public Event constructWebhookEvent(String payload, String sigHeader) {
        if (!isEnabled()) {
            log.warn("[Stripe SIMULADO] Webhook recibido — ignorado en modo simulado");
            return null;
        }
        try {
            return Webhook.constructEvent(payload, sigHeader, props.getStripe().getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw AppException.badRequest("Firma del webhook de Stripe inválida");
        }
    }

    private boolean isEnabled() {
        return props.getStripe().isEnabled()
               && props.getStripe().getSecretKey() != null
               && !props.getStripe().getSecretKey().isBlank();
    }
}
