package dev.pimon.newsletter.dto;

public record SubscriptionResultDto(
        String email,
        String status,
        boolean alreadySubscribed
) {}
