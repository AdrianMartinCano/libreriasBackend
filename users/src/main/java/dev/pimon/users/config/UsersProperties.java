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
}
