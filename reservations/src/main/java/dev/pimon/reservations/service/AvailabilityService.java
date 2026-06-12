package dev.pimon.reservations.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.reservations.config.ReservationsProperties;
import dev.pimon.reservations.dto.TimeSlotDto;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final BookingRepository       bookingRepo;
    private final ReservationsProperties  props;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Devuelve los slots del día para el servicio dado.
     * Marca como no disponibles los slots ya reservados.
     */
    public List<TimeSlotDto> slotsForDate(LocalDate date, ServiceOffering service) {
        LocalTime start    = LocalTime.parse(props.getWorkStart(), TIME_FMT);
        LocalTime end      = LocalTime.parse(props.getWorkEnd(), TIME_FMT);
        int       interval = props.getSlotIntervalMinutes();
        int       duration = service.getDurationMinutes();

        // Slots existentes ese día (sólo los no cancelados)
        Set<String> occupied = bookingRepo.findOccupiedTimesForDate(date)
                .stream().collect(Collectors.toSet());

        List<TimeSlotDto> slots = new ArrayList<>();
        LocalTime current = start;

        // El último slot posible es end - duration para que quepa la cita
        LocalTime lastStart = end.minusMinutes(duration);

        while (!current.isAfter(lastStart)) {
            String timeStr = current.format(TIME_FMT);
            slots.add(TimeSlotDto.of(timeStr, !occupied.contains(timeStr)));
            current = current.plusMinutes(interval);
        }

        return slots;
    }

    /** Comprueba que la fecha cae en un día laborable configurado. */
    public boolean isWorkDay(LocalDate date) {
        String dayName = date.getDayOfWeek().name(); // "MONDAY", "TUESDAY"...
        return props.getWorkDays().contains(dayName);
    }
}
