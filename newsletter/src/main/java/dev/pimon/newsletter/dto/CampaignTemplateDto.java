package dev.pimon.newsletter.dto;

import dev.pimon.newsletter.campaign.CampaignTemplate;

public record CampaignTemplateDto(
        String id,
        String name,
        String description
) {
    public static CampaignTemplateDto from(CampaignTemplate template) {
        return new CampaignTemplateDto(template.getId(), template.getDisplayName(), template.getDescription());
    }
}
