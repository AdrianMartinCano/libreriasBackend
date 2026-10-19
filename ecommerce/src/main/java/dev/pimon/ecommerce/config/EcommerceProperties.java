package dev.pimon.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuración en application.yml:
 *
 * libui:
 *   ecommerce:
 *     currency: EUR
 *     stripe:
 *       secret-key: sk_test_...         # clave secreta de Stripe
 *       webhook-secret: whsec_...       # secreto del webhook de Stripe
 *       enabled: true
 */
@Data
@ConfigurationProperties(prefix = "pimon.ecommerce")
public class EcommerceProperties {

    /** Moneda ISO 4217 (EUR, USD...) */
    private String currency = "eur";

    private Stripe stripe = new Stripe();

    private Shipping shipping = new Shipping();

    private Caducidad caducidad = new Caducidad();

    /**
     * Gastos de envío. Coste fijo por pedido; gratis si el subtotal alcanza {@code freeFrom}.
     */
    @Data
    public static class Shipping {
        /** Coste fijo de envío (0 = sin gastos de envío). */
        private BigDecimal cost = BigDecimal.ZERO;
        /** Envío gratis si el subtotal (sin envío) es >= a este valor. null = nunca gratis. */
        private BigDecimal freeFrom = null;
    }

    /**
     * Que hacer con los pedidos que se quedan esperando un pago que no llega.
     *
     * <h2>Por que hace falta</h2>
     *
     * {@code create-intent} descuenta el stock al crear el pedido, y eso es correcto: evita
     * vender dos veces la ultima pieza mientras un cliente teclea la tarjeta. Pero quien
     * llega al pago y se marcha deja ese stock retenido, y sin esto nadie lo devuelve.
     *
     * <h2>Viene APAGADO</h2>
     *
     * Cancela pedidos y mueve existencias. Que se encienda solo al actualizar la libreria
     * seria una sorpresa desagradable en una tienda en marcha, asi que se enciende a mano.
     */
    @Data
    public static class Caducidad {
        /** true = se revisan y cancelan los pendientes caducados. */
        private boolean enabled = false;

        /**
         * Cuanto se espera antes de dar un pedido por perdido.
         *
         * Media hora es holgado a proposito: un pago con verificacion del banco (3-D
         * Secure) puede tardar minutos, y quien deja la pestana abierta mientras busca la
         * tarjeta tambien. Quedarse corto cancela compras vivas.
         */
        private Duration espera = Duration.ofMinutes(30);

        /** Cada cuanto se mira. Cinco minutos: no corre ninguna prisa. */
        private long intervaloMs = 300_000;
    }

    @Data
    public static class Stripe {
        /** Clave secreta: sk_test_... (test) o sk_live_... (producción) */
        private String  secretKey     = "";
        /** Secreto del webhook de Stripe Dashboard */
        private String  webhookSecret = "";
        /** false = modo simulado sin llamadas reales a Stripe */
        private boolean enabled       = false;
    }
}
