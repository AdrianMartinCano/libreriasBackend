package dev.pimon.newsletter.dto;

import dev.pimon.newsletter.entity.NewsletterCampaign;

public record CampaignDto(
        String id,
        String subject,
        String body,
        int recipientCount,
        String sentAt
) {
    public static CampaignDto from(NewsletterCampaign c) {
        return new CampaignDto(
                c.getId(),
                c.getSubject(),
                c.getBody(),
                c.getRecipientCount(),
                c.getSentAt().toString()
        );
    }
}
