package dev.pimon.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
