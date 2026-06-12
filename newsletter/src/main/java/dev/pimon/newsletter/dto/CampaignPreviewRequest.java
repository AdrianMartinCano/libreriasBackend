package dev.pimon.newsletter.dto;

/** Sin validación: la previsualización debe funcionar también con campos vacíos mientras se escribe. */
public record CampaignPreviewRequest(
        String subject,
        String body,
        String templateId
) {}
