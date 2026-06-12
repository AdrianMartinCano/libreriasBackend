package dev.pimon.reservations.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.exception.AppException;
import dev.pimon.reservations.dto.BookingDto;
import dev.pimon.reservations.dto.CreateBookingRequest;
import dev.pimon.reservations.dto.UpdateBookingStatusRequest;
import dev.pimon.reservations.entity.Booking;
import dev.pimon.reservations.entity.Booking.BookingStatus;
import dev.pimon.reservations.entity.ServiceOffering;
import dev.pimon.reservations.repository.BookingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository      repo;
    private final ServiceOfferingService serviceOfferingService;
    private final AvailabilityService    availabilityService;

    @Transactional
    public BookingDto create(CreateBookingRequest req) {
        ServiceOffering service = serviceOfferingService.findOrThrow(req.serviceId());
        LocalDate date = LocalDate.parse(req.date());

        if (!availabilityService.isWorkDay(date)) {
            throw AppException.conflict("Ese día no hay disponibilidad");
        }

        boolean slotTaken = repo.findOccupiedTimesForDate(date).contains(req.time());
        if (slotTaken) {
            throw AppException.conflict(
                    "El slot " + req.time() + " del " + req.date() + " ya está reservado");
        }

        Booking booking = new Booking();
        booking.setService(service);
        booking.setDate(date);
        booking.setTime(req.time());
        booking.setClientName(req.clientName());
        booking.setClientEmail(req.clientEmail());
        booking.setClientPhone(req.clientPhone());
        booking.setNotes(req.notes());

        return BookingDto.from(repo.save(booking));
    }

    public Page<BookingDto> listAll(String status, Pageable pageable) {
        if (status != null) {
            BookingStatus s = parseStatus(status);
            return repo.findByStatusOrderByDateAscTimeAsc(s, pageable).map(BookingDto::from);
        }
        return repo.findAllByOrderByDateDescTimeAsc(pageable).map(BookingDto::from);
    }

    public BookingDto findById(String id) {
        return BookingDto.from(findOrThrow(id));
    }

    public List<BookingDto> findByEmail(String email) {
        return repo.findByClientEmailOrderByDateDescTimeAsc(email)
                .stream().map(BookingDto::from).toList();
    }

    @Transactional
    public BookingDto updateStatus(String id, UpdateBookingStatusRequest req) {
        Booking booking = findOrThrow(id);
        booking.setStatus(parseStatus(req.status()));
        return BookingDto.from(repo.save(booking));
    }

    private Booking findOrThrow(String id) {
        return repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Reserva no encontrada con id: " + id));
    }

    private BookingStatus parseStatus(String raw) {
        try {
            return BookingStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Estado inválido: " + raw);
        }
    }
}
