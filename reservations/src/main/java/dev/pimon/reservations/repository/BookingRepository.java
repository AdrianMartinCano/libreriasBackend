package dev.pimon.reservations.repository;

import dev.pimon.reservations.entity.Booking;
import dev.pimon.reservations.entity.Booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    /** Reservas activas (no canceladas) para una fecha — para calcular disponibilidad. */
    @Query("SELECT b.time FROM Booking b WHERE b.date = :date AND b.status <> 'CANCELLED'")
    List<String> findOccupiedTimesForDate(@Param("date") LocalDate date);

    /** Listado admin filtrable por estado. */
    Page<Booking> findByStatusOrderByDateAscTimeAsc(BookingStatus status, Pageable pageable);

    /** Listado admin sin filtro de estado. */
    Page<Booking> findAllByOrderByDateDescTimeAsc(Pageable pageable);

    /** Reservas de un cliente por email. */
    List<Booking> findByClientEmailOrderByDateDescTimeAsc(String email);
}
