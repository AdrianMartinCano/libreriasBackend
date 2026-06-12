package dev.pimon.newsletter.dto;

import dev.pimon.newsletter.entity.NewsletterSubscriber;

public record SubscriberDto(
        String id,
        String email,
        String source,
        String status,
        String createdAt,
        String confirmedAt
) {
    public static SubscriberDto from(NewsletterSubscriber s) {
        return new SubscriberDto(
                s.getId(),
                s.getEmail(),
                s.getSource(),
                s.getStatus().name().toLowerCase(),
                s.getCreatedAt().toString(),
                s.getConfirmedAt() != null ? s.getConfirmedAt().toString() : null
        );
    }
}
