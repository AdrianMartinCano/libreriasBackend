package dev.pimon.newsletter.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.email.config.EmailProperties;
import dev.pimon.email.service.EmailService;
import dev.pimon.newsletter.campaign.CampaignTemplate;
import dev.pimon.newsletter.config.NewsletterProperties;
import dev.pimon.newsletter.dto.CampaignDto;
import dev.pimon.newsletter.dto.CampaignPreviewDto;
import dev.pimon.newsletter.dto.CampaignPreviewRequest;
import dev.pimon.newsletter.dto.CampaignTemplateDto;
import dev.pimon.newsletter.dto.SendCampaignRequest;
import dev.pimon.newsletter.dto.SubscribeRequest;
import dev.pimon.newsletter.dto.SubscriberDto;
import dev.pimon.newsletter.dto.SubscriptionResultDto;
import dev.pimon.newsletter.entity.NewsletterCampaign;
import dev.pimon.newsletter.entity.NewsletterSubscriber;
import dev.pimon.newsletter.entity.NewsletterSubscriber.SubscriberStatus;
import dev.pimon.newsletter.repository.NewsletterCampaignRepository;
import dev.pimon.newsletter.repository.NewsletterSubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NewsletterSubscriptionService {

    private final NewsletterSubscriberRepository repo;
    private final NewsletterCampaignRepository   campaignRepo;
    private final NewsletterProperties           props;
    private final EmailProperties                emailProps;
    private final EmailService                   emailService;

    @Transactional
    public SubscriptionResultDto subscribe(SubscribeRequest req) {
        String email = req.email().trim().toLowerCase();

        NewsletterSubscriber sub = repo.findByEmail(email).orElse(null);

        if (sub != null && sub.getStatus() == SubscriberStatus.CONFIRMED) {
            return new SubscriptionResultDto(email, "confirmed", true);
        }

        if (sub == null) {
            sub = new NewsletterSubscriber();
            sub.setEmail(email);
            sub.setUnsubscribeToken(UUID.randomUUID().toString());
        }

        sub.setSource(req.source());
        sub.setStatus(SubscriberStatus.PENDING);
        sub.setConfirmationToken(UUID.randomUUID().toString());
        if (sub.getUnsubscribeToken() == null) {
            sub.setUnsubscribeToken(UUID.randomUUID().toString());
        }
        repo.save(sub);

        sendConfirmationEmail(sub);

        return new SubscriptionResultDto(email, "pending", false);
    }

    @Transactional
    public void confirm(String token) {
        NewsletterSubscriber sub = repo.findByConfirmationToken(token)
                .orElseThrow(() -> AppException.notFound("Enlace de confirmación inválido o caducado"));

        sub.setStatus(SubscriberStatus.CONFIRMED);
        sub.setConfirmedAt(LocalDateTime.now());
        sub.setConfirmationToken(null);
        repo.save(sub);

        sendWelcomeEmail(sub);
    }

    @Transactional
    public void unsubscribe(String token) {
        NewsletterSubscriber sub = repo.findByUnsubscribeToken(token)
                .orElseThrow(() -> AppException.notFound("Enlace de baja inválido"));

        sub.setStatus(SubscriberStatus.UNSUBSCRIBED);
        sub.setUnsubscribedAt(LocalDateTime.now());
        repo.save(sub);
    }

    public Page<SubscriberDto> list(String status, Pageable pageable) {
        if (status != null) {
            return repo.findByStatus(parseStatus(status), pageable).map(SubscriberDto::from);
        }
        return repo.findAll(pageable).map(SubscriberDto::from);
    }

    public String exportCsv(String status) {
        List<NewsletterSubscriber> subscribers = status != null
                ? repo.findByStatus(parseStatus(status), Pageable.unpaged()).getContent()
                : repo.findAll();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("email,source,status,createdAt,confirmedAt");
            for (NewsletterSubscriber s : subscribers) {
                writer.printf("%s,%s,%s,%s,%s%n",
                        s.getEmail(),
                        s.getSource() != null ? s.getSource() : "",
                        s.getStatus().name().toLowerCase(),
                        s.getCreatedAt(),
                        s.getConfirmedAt() != null ? s.getConfirmedAt() : "");
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    @Transactional
    public CampaignDto sendCampaign(SendCampaignRequest req) {
        List<NewsletterSubscriber> recipients =
                repo.findByStatus(SubscriberStatus.CONFIRMED, Pageable.unpaged()).getContent();

        NewsletterCampaign campaign = new NewsletterCampaign();
        campaign.setSubject(req.subject());
        campaign.setBody(req.body());
        campaign.setRecipientCount(recipients.size());
        campaign.setSentAt(LocalDateTime.now());
        campaignRepo.save(campaign);

        for (NewsletterSubscriber sub : recipients) {
            sendCampaignEmail(sub, req);
        }

        return CampaignDto.from(campaign);
    }

    public Page<CampaignDto> listCampaigns(Pageable pageable) {
        return campaignRepo.findAllByOrderBySentAtDesc(pageable).map(CampaignDto::from);
    }

    /** Plantillas de campaña disponibles, para que el admin elija una al redactar. */
    public List<CampaignTemplateDto> listCampaignTemplates() {
        return Arrays.stream(CampaignTemplate.values()).map(CampaignTemplateDto::from).toList();
    }

    /**
     * Renderiza el HTML que se enviaría para esta campaña, sin enviarlo.
     * Sin plantilla, el resultado es el contenido tal cual ("a pelo").
     */
    public CampaignPreviewDto renderPreview(CampaignPreviewRequest req) {
        String subject = req.subject() != null ? req.subject() : "";
        String body = req.body() != null ? req.body() : "";

        Optional<CampaignTemplate> template = CampaignTemplate.fromId(req.templateId());
        if (template.isEmpty()) {
            return new CampaignPreviewDto(body);
        }

        String html = emailService.renderTemplate(
                template.get().getTemplateFile(),
                Map.of("subject", subject, "body", body, "unsubscribeUrl", "#", "brand", brandVars()));
        return new CampaignPreviewDto(html);
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
                Map.of("subject", req.subject(), "body", req.body(), "unsubscribeUrl", unsubscribeUrl, "brand", brandVars()));
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

    private void sendConfirmationEmail(NewsletterSubscriber sub) {
        String confirmUrl = props.getBaseUrl() + "/api/newsletter/confirm?token=" + sub.getConfirmationToken();
        String unsubscribeUrl = props.getBaseUrl() + "/api/newsletter/unsubscribe?token=" + sub.getUnsubscribeToken();
        emailService.sendTemplate(
                sub.getEmail(),
                "Confirma tu suscripción",
                "newsletter-confirm",
                Map.of("confirmUrl", confirmUrl, "unsubscribeUrl", unsubscribeUrl, "brand", brandVars()));
    }

    private void sendWelcomeEmail(NewsletterSubscriber sub) {
        String unsubscribeUrl = props.getBaseUrl() + "/api/newsletter/unsubscribe?token=" + sub.getUnsubscribeToken();
        emailService.sendTemplate(
                sub.getEmail(),
                "¡Suscripción confirmada!",
                "newsletter-welcome",
                Map.of("unsubscribeUrl", unsubscribeUrl, "brand", brandVars()));
    }

    private SubscriberStatus parseStatus(String raw) {
        try {
            return SubscriberStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Estado inválido: " + raw);
        }
    }
}
