package dev.pimon.email.service;

import jakarta.annotation.Nullable;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.email.config.EmailProperties;
import dev.pimon.email.dto.BookingEmailData;
import dev.pimon.email.dto.ContactFormData;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender  mailSender;
    private final TemplateEngine  emailTemplateEngine;
    private final EmailProperties props;

    /* ══════════════════════════════════════════════════════════════
       CONFIRMACIÓN DE RESERVA — al cliente
       ══════════════════════════════════════════════════════════════ */

    /** Usa la plantilla por defecto. */
    public void sendBookingConfirmation(BookingEmailData data) {
        sendBookingConfirmation(data, null);
    }

    /** Si customHtml no es null, usa ese HTML en vez de la plantilla. */
    public void sendBookingConfirmation(BookingEmailData data, @Nullable String customHtml) {
        String subject = "Reserva recibida — " + data.getServiceName();
        String html = customHtml != null
            ? customHtml
            : render("booking-confirmation", Map.of("booking", data, "studio", studioVars()));
        send(data.getClientEmail(), subject, html);
    }

    /* ══════════════════════════════════════════════════════════════
       CAMBIO DE ESTADO — al cliente
       ══════════════════════════════════════════════════════════════ */

    public void sendBookingStatusUpdate(BookingEmailData data) {
        sendBookingStatusUpdate(data, null);
    }

    public void sendBookingStatusUpdate(BookingEmailData data, @Nullable String customHtml) {
        String subject = switch (data.getStatus()) {
            case "confirmed"  -> "✓ Cita confirmada — " + data.getServiceName();
            case "cancelled"  -> "Cita cancelada — " + data.getServiceName();
            case "completed"  -> "¡Gracias por tu visita!";
            default           -> "Actualización de tu reserva";
        };
        String html = customHtml != null
            ? customHtml
            : render("booking-status", Map.of("booking", data, "studio", studioVars()));
        send(data.getClientEmail(), subject, html);
    }

    /* ══════════════════════════════════════════════════════════════
       NOTIFICACIÓN ADMIN — nueva reserva
       ══════════════════════════════════════════════════════════════ */

    public void sendBookingAdminNotification(BookingEmailData data) {
        sendBookingAdminNotification(data, null);
    }

    public void sendBookingAdminNotification(BookingEmailData data, @Nullable String customHtml) {
        if (props.getAdminEmail().isBlank()) return;
        String subject = "[Nueva reserva] " + data.getClientName() + " — " + data.getServiceName();
        String html = customHtml != null
            ? customHtml
            : render("booking-admin", Map.of("booking", data));
        send(props.getAdminEmail(), subject, html);
    }

    /* ══════════════════════════════════════════════════════════════
       FORMULARIO DE CONTACTO
       ══════════════════════════════════════════════════════════════ */

    public void sendContactForm(ContactFormData data) {
        sendContactForm(data, null);
    }

    public void sendContactForm(ContactFormData data, @Nullable String customHtml) {
        if (props.getAdminEmail().isBlank()) {
            log.warn("[EmailService] adminEmail no configurado");
            return;
        }
        String subject = "[Contacto] " + data.getSubject() + " — " + data.getFromName();
        String html = customHtml != null
            ? customHtml
            : render("contact-form", Map.of("form", data));
        send(props.getAdminEmail(), subject, html);
    }

    /* ══════════════════════════════════════════════════════════════
       GENÉRICO — plantilla por nombre o HTML directo
       ══════════════════════════════════════════════════════════════ */

    /** Renderiza una plantilla Thymeleaf y la envía. */
    public void sendTemplate(String to, String subject, String template, Map<String, Object> vars) {
        send(to, subject, render(template, vars));
    }

    /** Envía un HTML arbitrario (sin plantilla). */
    public void sendHtml(String to, String subject, String html) {
        send(to, subject, html);
    }

    /** Renderiza una plantilla Thymeleaf sin enviarla (p.ej. para previsualizaciones). */
    public String renderTemplate(String template, Map<String, Object> vars) {
        return render(template, vars);
    }

    /* ══════════════════════════════════════════════════════════════
       INTERNOS
       ══════════════════════════════════════════════════════════════ */

    private String render(String template, Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        return emailTemplateEngine.process(template, ctx);
    }

    private void send(String to, String subject, String html) {
        if (!props.isEnabled()) {
            log.info("[EmailService] desactivado — to={} subject={}", to, subject);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(props.getFrom(), props.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("[EmailService] enviado → {} | {}", to, subject);
        } catch (Exception e) {
            log.error("[EmailService] error enviando a {}: {}", to, e.getMessage());
        }
    }

    private Map<String, String> studioVars() {
        return Map.of(
            "name",  props.getFromName(),
            "email", props.getAdminEmail().isBlank() ? props.getFrom() : props.getAdminEmail()
        );
    }
}
