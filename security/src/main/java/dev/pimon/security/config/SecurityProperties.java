package dev.pimon.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración de seguridad en application.yml:
 *
 * libui:
 *   security:
 *     jwt-secret: "mi-secreto-de-al-menos-32-caracteres"
 *     access-token-expiration: 86400000
 *     public-paths:
 *       - "/api/auth/**"
 *     cors:
 *       allowed-origins:
 *         - "http://localhost:4200"
 *         - "https://miapp.com"
 */
@Data
@ConfigurationProperties(prefix = "pimon.security")
public class SecurityProperties {

    private String jwtSecret = "libui-default-secret-change-in-production-32chars";
    private long   accessTokenExpiration = 86_400_000L;

    private List<String> publicPaths = List.of(
            "/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/actuator/health"
    );

    private Cors cors = new Cors();

    @Data
    public static class Cors {
        /**
         * Orígenes permitidos para CORS.
         * En desarrollo: http://localhost:4200
         * En producción: https://tudominio.com
         * Para permitir todos (no recomendado en prod): ["*"]
         */
        private List<String> allowedOrigins = List.of(
                "http://localhost:4200",
                "http://localhost:4201",
                "http://localhost:4202"
        );

        private List<String> allowedMethods = List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        );
    }
}
