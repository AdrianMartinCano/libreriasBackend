package dev.pimon.reservations.dto;

import dev.pimon.reservations.entity.StaffMember;

/**
 * Vista pública de un trabajador para el flujo de reserva. NO expone horario, vínculo de usuario ni
 * vacaciones — solo lo necesario para que el cliente elija profesional.
 */
public record StaffDto(
        String id,
        String name,
        String color,
        String imageUrl
) {
    public static StaffDto from(StaffMember s) {
        return new StaffDto(s.getId(), s.getName(), s.getColor(), s.getImageUrl());
    }
}
