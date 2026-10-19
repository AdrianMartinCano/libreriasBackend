package dev.pimon.newsletter.service;

import dev.pimon.email.config.EmailProperties;
import dev.pimon.email.service.EmailService;
import dev.pimon.newsletter.campaign.CampaignTemplate;
import dev.pimon.newsletter.config.NewsletterProperties;
import dev.pimon.newsletter.dto.SendCampaignRequest;
import dev.pimon.newsletter.entity.NewsletterSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Envía los emails de una campaña de forma <b>asíncrona</b> en un hilo acotado
 * ({@code newsletterExecutor}), para que el endpoint de admin responda al instante aunque haya
 * miles de destinatarios. Resiliente por destinatario: un fallo se registra y no detiene el resto.
 *
 * <p>Vive en un bean aparte (no en {@code NewsletterSubscriptionService}) porque {@code @Async} solo
 * surte efecto cuando el método se invoca <b>a través del proxy</b> de Spring, no en una
 * auto-invocación dentro de la misma clase.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignDispatcher {

    private final EmailService         emailService;
    private final NewsletterProperties props;
    private final EmailProperties      emailProps;

    @Async("newsletterExecutor")
    public void dispatch(List<NewsletterSubscriber> recipients, SendCampaignRequest req) {
        int sent = 0, failed = 0;
        for (NewsletterSubscriber sub : recipients) {
            try {
                sendCampaignEmail(sub, req);
                sent++;
            } catch (Exception e) {
                failed++;
                log.warn("Campaña '{}': fallo al enviar a {} — {}", req.subject(), sub.getEmail(), e.getMessage());
            }
        }
        log.info("Campaña '{}' enviada (async): {} OK, {} fallidos de {} destinatarios",
                req.subject(), sent, failed, recipients.size());
    }

    private void sendCampaignEmail(NewsletterSubscriber sub, SendCampaignRequest req) {
        String unsubscribeUrl = props.getBaseUrl() + "/api/newsletter/unsubscribe?token=" + sub.getUnsubscribeToken();

        Optional<CampaignTemplate> template = CampaignTemplate.fromId(req.templateId());
        if (template.isEmpty()) {
            emailService.sendHtml(sub.getEmail(), req.subject(), req.body());
            return;
        }
        emailService.sendTemplate(
                sub.getEmail(),
                req.subject(),
                template.get().getTemplateFile(),
                Map.of("subject", req.subject(), "body", req.body(),
                        "unsubscribeUrl", unsubscribeUrl, "brand", brandVars()));
    }

    /** Datos de marca para parametrizar las plantillas de email genéricas. */
    private Map<String, Object> brandVars() {
        String name = emailProps.getBrandName().isBlank() ? emailProps.getFromName() : emailProps.getBrandName();
        return Map.of(
                "name", name,
                "primaryColor", emailProps.getPrimaryColor(),
                "accentColor", emailProps.getAccentColor(),
                "logoUrl", emailProps.getLogoUrl());
    }
}
