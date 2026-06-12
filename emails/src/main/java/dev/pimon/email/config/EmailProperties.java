package dev.pimon.email.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración en application.yml:
 *
 * libui:
 *   email:
 *     enabled: true
 *     from: no-reply@miapp.com
 *     from-name: Mi Aplicación
 *     admin-email: admin@miapp.com
 *     brand-name: Mi Empresa
 *     primary-color: "#0f0f0f"
 *     accent-color: "#c9a84c"
 *     logo-url: https://miapp.com/logo.png
 *
 * spring:
 *   mail:
 *     host: smtp.gmail.com
 *     port: 587
 *     username: ${MAIL_USER}
 *     password: ${MAIL_PASSWORD}
 *     properties:
 *       mail.smtp.auth: true
 *       mail.smtp.starttls.enable: true
 */
@Data
@ConfigurationProperties(prefix = "pimon.email")
public class EmailProperties {

    /** Activar/desactivar envío de emails. false = solo log, no envía. */
    private boolean enabled = true;

    /** Dirección del remitente. */
    private String from = "no-reply@localhost";

    /** Nombre visible del remitente. */
    private String fromName = "Notificaciones";

    /** Email del administrador (para copias de formularios de contacto). */
    private String adminEmail = "";

    /** Nombre de marca mostrado en plantillas de email. Si está vacío, las plantillas usan {@link #fromName}. */
    private String brandName = "";

    /** Color principal (cabeceras, fondos oscuros) usado en plantillas de email. */
    private String primaryColor = "#0f0f0f";

    /** Color de acento (títulos, enlaces destacados) usado en plantillas de email. */
    private String accentColor = "#c9a84c";

    /** URL del logo a mostrar en plantillas de email. Si está vacío, no se muestra logo. */
    private String logoUrl = "";
}
