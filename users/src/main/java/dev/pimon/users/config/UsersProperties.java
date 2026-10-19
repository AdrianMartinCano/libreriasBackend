package dev.pimon.users.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de libui-users en application.yml:
 *
 * libui:
 *   users:
 *     create-default-admin: true
 *     admin-email: admin@miapp.com
 *     admin-password: Admin1234!
 *     admin-name: Administrador
 */
@Data
@ConfigurationProperties(prefix = "pimon.users")
public class UsersProperties {

    /** Si true, crea un admin al arrancar si no hay ningún usuario en la BD */
    private boolean createDefaultAdmin = true;

    private String adminEmail    = "admin@example.com";
    private String adminPassword = "Admin1234!";
    private String adminName     = "Administrador";

    private Recuperacion recuperacion = new Recuperacion();

    /**
     * Recuperar la contrasena olvidada.
     *
     * <h2>Una hora, ni mas ni menos</h2>
     *
     * Un enlace de recuperacion acaba en la bandeja de entrada y ahi se queda: cuanto mas
     * vive, mas tiempo hay para que alguien lo encuentre —un movil prestado, un correo
     * reenviado sin pensar—. Y demasiado corto tampoco vale: quien lo pide desde el movil
     * y va a por el portatil necesita un rato.
     */
    @Data
    public static class Recuperacion {
        /** Cuanto vale el enlace desde que se emite. */
        private java.time.Duration validez = java.time.Duration.ofHours(1);
    }
}
