package dev.pimon.reservations.dto;

public record TimeSlotDto(
        String  time,
        boolean available,
        String  label
) {
    public static TimeSlotDto of(String time, boolean available) {
        return new TimeSlotDto(time, available, time + " h");
    }
}
