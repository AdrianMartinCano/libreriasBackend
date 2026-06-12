package dev.pimon.reservations.dto;

import dev.pimon.reservations.entity.Booking;

public record BookingDto(
        String             id,
        ServiceOfferingDto service,
        String             date,
        String             time,
        String             clientName,
        String             clientEmail,
        String             clientPhone,
        String             notes,
        String             status,
        String             createdAt
) {
    public static BookingDto from(Booking b) {
        return new BookingDto(
                b.getId(),
                ServiceOfferingDto.from(b.getService()),
                b.getDate().toString(),
                b.getTime(),
                b.getClientName(),
                b.getClientEmail(),
                b.getClientPhone(),
                b.getNotes(),
                b.getStatus().name().toLowerCase(),
                b.getCreatedAt().toString()
        );
    }
}
