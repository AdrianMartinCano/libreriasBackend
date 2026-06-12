package dev.pimon.newsletter.dto;

import jakarta.validation.constraints.NotBlank;

public record SendCampaignRequest(
        @NotBlank String subject,
        @NotBlank String body,
        /** Id de {@link dev.pimon.newsletter.campaign.CampaignTemplate}. Vacío/null = enviar el contenido tal cual. */
        String templateId
) {}
