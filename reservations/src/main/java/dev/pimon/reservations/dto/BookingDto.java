package dev.pimon.reservations.dto;

import dev.pimon.reservations.entity.Booking;
import dev.pimon.reservations.entity.StaffMember;

public record BookingDto(
        String             id,
        ServiceOfferingDto service,
        String             date,
        String             time,
        String             clientName,
        String             clientEmail,
        String             clientPhone,
        String             notes,
        Integer            durationMinutes,
        String             status,
        String             createdAt,
        // Trabajador asignado (null en datos sin asignar). staffName/staffColor para pintar la columna.
        String             staffId,
        String             staffName,
        String             staffColor
) {
    public static BookingDto from(Booking b) {
        StaffMember staff = b.getStaff();
        return new BookingDto(
                b.getId(),
                ServiceOfferingDto.from(b.getService()),
                b.getDate().toString(),
                b.getTime(),
                b.getClientName(),
                b.getClientEmail(),
                b.getClientPhone(),
                b.getNotes(),
                b.getDurationMinutes(),
                b.getStatus().name().toLowerCase(),
                b.getCreatedAt().toString(),
                staff != null ? staff.getId()    : null,
                staff != null ? staff.getName()  : null,
                staff != null ? staff.getColor() : null
        );
    }
}
