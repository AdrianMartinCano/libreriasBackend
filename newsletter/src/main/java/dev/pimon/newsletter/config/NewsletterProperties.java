package dev.pimon.newsletter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración en application.yml:
 *
 * pimon:
 *   newsletter:
 *     base-url: https://api.midominio.com
 *     confirm-redirect-url: https://www.midominio.com/?newsletter=confirmado
 *     unsubscribe-redirect-url: https://www.midominio.com/?newsletter=baja
 *     error-redirect-url: https://www.midominio.com/?newsletter=error
 */
@Data
@ConfigurationProperties(prefix = "pimon.newsletter")
public class NewsletterProperties {

    /** URL pública base de esta API, usada para construir los enlaces de confirmación/baja en los emails. */
    private String baseUrl = "http://localhost:8080";

    /** URL del frontend a la que redirigir tras confirmar la suscripción. */
    private String confirmRedirectUrl = "/";

    /** URL del frontend a la que redirigir tras darse de baja. */
    private String unsubscribeRedirectUrl = "/";

    /** URL del frontend a la que redirigir si el enlace de confirmación/baja es inválido o ya se usó. */
    private String errorRedirectUrl = "/";
}
