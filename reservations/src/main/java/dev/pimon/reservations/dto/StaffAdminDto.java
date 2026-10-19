package dev.pimon.reservations.dto;

import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.entity.StaffMember;
import dev.pimon.reservations.entity.WorkSlot;
import dev.pimon.reservations.settings.TimeRange;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vista admin completa de un trabajador (CRUD del panel del jefe). Reconstruye el horario plano
 * ({@code workSlots}) al formato {@code Map<día, tramos>} que ya consume el front, igual que
 * {@code ReservationsSettings.weeklyHours}.
 */
public record StaffAdminDto(
        String id,
        String name,
        String color,
        String imageUrl,
        boolean active,
        int sortOrder,
        String userId,                                  // null = sin login
        boolean customHours,
        Map<DayOfWeek, List<TimeRange>> weeklyHours,    // reconstruido desde workSlots
        List<String> blockedDates,                      // ISO 'YYYY-MM-DD'
        List<String> serviceIds                         // vacío = hace todos
) {
    public static StaffAdminDto from(StaffMember s) {
        Map<DayOfWeek, List<TimeRange>> weekly = s.getWorkSlots().stream().collect(Collectors.groupingBy(
                WorkSlot::getDayOfWeek,
                Collectors.mapping(ws -> new TimeRange(ws.getStart(), ws.getEnd()), Collectors.toList())));

        List<String> blocked = s.getBlockedDates().stream()
                .map(LocalDate::toString).sorted().toList();

        List<String> serviceIds = s.getServices().stream()
                .map(ServiceOffering::getId).toList();

        return new StaffAdminDto(
                s.getId(), s.getName(), s.getColor(), s.getImageUrl(),
                s.isActive(), s.getSortOrder(), s.getUserId(), s.isCustomHours(),
                weekly, blocked, serviceIds);
    }
}
