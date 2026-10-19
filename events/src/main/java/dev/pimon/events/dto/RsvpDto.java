package dev.pimon.events.dto;

import dev.pimon.events.entity.Rsvp;

import java.time.LocalDateTime;

/** Vista de una confirmación devuelta al cliente. */
public record RsvpDto(
        String id,
        String eventId,
        String name,
        boolean attending,
        int guests,
        LocalDateTime createdAt
) {
    public static RsvpDto from(Rsvp r) {
        return new RsvpDto(r.getId(), r.getEventId(), r.getName(), r.isAttending(), r.getGuests(), r.getCreatedAt());
    }
}
