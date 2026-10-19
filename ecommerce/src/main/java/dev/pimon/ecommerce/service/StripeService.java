package dev.pimon.ecommerce.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.exception.AppException;
import dev.pimon.ecommerce.config.EcommerceProperties;
import org.springframework.stereotype.Service;

import java.util.Optional;
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
     * Devuelve el dinero de un pago.
     *
     * IMPORTANTE PARA QUIEN LLAME: si esto lanza, el dinero NO ha salido. El estado del
     * pedido no debe marcarse como reembolsado hasta que este método vuelva sin error.
     * Un pedido en REFUNDED cuyo dinero sigue en la cuenta es peor que no tener el botón:
     * el cliente recibe un correo diciendo que se le ha devuelto y no es verdad.
     *
     * IDEMPOTENCIA: se manda una clave derivada del PaymentIntent, así que si esta misma
     * llamada se repite —doble clic, un reintento, dos pestañas del panel— Stripe
     * devuelve el reembolso que ya hizo en lugar de crear uno nuevo. Sin ella, dos
     * pulsaciones seguidas podrían intentar devolver el dinero dos veces.
     *
     * @param paymentIntentId el {@code pi_...} guardado en el pedido
     * @param amountInCents   importe a devolver; el total del pedido para uno completo
     * @return el id del reembolso en Stripe ({@code re_...}), o null en modo simulado
     * @throws AppException si Stripe rechaza la operación
     */
    public String refund(String paymentIntentId, long amountInCents) {
        if (!isEnabled()) {
            log.warn("[Stripe SIMULADO] Reembolso de {}€ para {} — no sale dinero de ninguna parte",
                     amountInCents / 100.0, paymentIntentId);
            return null;
        }

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw AppException.badRequest(
                    "Este pedido no tiene pago asociado en Stripe: no hay nada que devolver");
        }
        // Un pago simulado no existe en Stripe. Sin esta comprobación, un pedido creado
        // en modo demo y reembolsado en producción daría un error críptico de Stripe.
        if (paymentIntentId.startsWith("pi_demo_")) {
            throw AppException.badRequest(
                    "Este pedido se creó con Stripe en modo simulado: no se puede reembolsar");
        }

        Stripe.apiKey = props.getStripe().getSecretKey();
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amountInCents)
                    .build();

            RequestOptions opciones = RequestOptions.builder()
                    .setIdempotencyKey("refund_" + paymentIntentId)
                    .build();

            Refund refund = Refund.create(params, opciones);
            log.info("Reembolso {} creado en Stripe: {}€ de {}",
                     refund.getId(), amountInCents / 100.0, paymentIntentId);
            return refund.getId();

        } catch (StripeException e) {
            // Se registra con el detalle y se propaga con un mensaje legible: aquí el
            // fallo importa de verdad, porque significa que el cliente NO ha cobrado.
            log.error("Stripe rechazó el reembolso de {}: {}", paymentIntentId, e.getMessage());
            throw AppException.internalError("No se ha podido devolver el dinero: " + e.getMessage());
        }
    }

    /**
     * En que estado esta un pago, segun Stripe.
     *
     * <h2>Para que existe: no cancelar un pedido que SI se pago</h2>
     *
     * Lo usa la caducidad de pedidos pendientes antes de dar uno por perdido. Si el
     * webhook se perdio, el pedido sigue en PENDING_PAYMENT aunque el dinero entrara;
     * cancelarlo entonces seria lo peor que puede pasar en toda la tienda: cobrado,
     * cancelado y con el stock devuelto a la venta.
     *
     * <h2>Ante la duda, NO se dice que no esta pagado</h2>
     *
     * Si Stripe no contesta —red, caida, clave mal— se devuelve {@code empty} en vez de
     * suponer nada. Quien llama debe tratarlo como "no lo se" y dejar el pedido en paz. Un
     * pedido colgado con su stock retenido se arregla a mano; un cobro sin pedido, no.
     *
     * @return el estado de Stripe ({@code succeeded}, {@code requires_payment_method}...),
     *         o vacio si no se ha podido saber.
     */
    public Optional<String> estadoDelPago(String paymentIntentId) {
        if (!isEnabled() || paymentIntentId == null || paymentIntentId.isBlank()
                || paymentIntentId.startsWith("pi_demo_")) {
            // En modo simulado no hay nada que consultar y tampoco hay dinero de por medio.
            return Optional.empty();
        }

        Stripe.apiKey = props.getStripe().getSecretKey();
        try {
            return Optional.ofNullable(PaymentIntent.retrieve(paymentIntentId).getStatus());
        } catch (StripeException e) {
            log.warn("No se ha podido consultar el pago {} en Stripe: {}",
                     paymentIntentId, e.getMessage());
            return Optional.empty();
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
