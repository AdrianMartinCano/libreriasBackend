package dev.pimon.reservations.repository;

import dev.pimon.reservations.entity.Booking;
import dev.pimon.reservations.entity.Booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    /** Reservas activas (no canceladas) para una fecha — para calcular disponibilidad. */
    @Query("SELECT b.time FROM Booking b WHERE b.date = :date AND b.status <> 'CANCELLED'")
    List<String> findOccupiedTimesForDate(@Param("date") LocalDate date);

    /**
     * Reservas activas de un día con su servicio cargado (JOIN FETCH) — para comprobar solapes por
     * intervalo (inicio + duración real o la del servicio) sin incurrir en N+1.
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.service WHERE b.date = :date AND b.status <> 'CANCELLED'")
    List<Booking> findActiveByDateWithService(@Param("date") LocalDate date);

    /**
     * Reservas activas de un día <b>de un trabajador concreto</b> — huecos y solape por persona.
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.service " +
           "WHERE b.date = :date AND b.staff.id = :staffId AND b.status <> 'CANCELLED'")
    List<Booking> findActiveByDateAndStaff(@Param("date") LocalDate date, @Param("staffId") String staffId);

    /**
     * Reservas activas de un día con servicio y trabajador cargados — para la agregación "cualquiera"
     * (se agrupa por trabajador en memoria) y para el timeline del jefe. {@code LEFT JOIN} en staff por
     * si hubiera citas sin asignar (datos legacy aún sin backfill).
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.service LEFT JOIN FETCH b.staff " +
           "WHERE b.date = :date AND b.status <> 'CANCELLED' ORDER BY b.time ASC")
    List<Booking> findActiveByDateWithStaff(@Param("date") LocalDate date);

    /** Nº de citas activas de un trabajador en un día — balanceo al asignar "cualquiera". */
    long countByStaff_IdAndDateAndStatusNot(String staffId, LocalDate date, BookingStatus status);

    /** ¿Tiene el trabajador alguna reserva? — impide borrarlo (debe desactivarse). */
    boolean existsByStaff_Id(String staffId);

    /** Backfill: asigna a un trabajador todas las reservas aún sin asignar. */
    @Modifying
    @Query("UPDATE Booking b SET b.staff = :staff WHERE b.staff IS NULL")
    int assignNullStaffTo(@Param("staff") dev.pimon.reservations.entity.StaffMember staff);

    /**
     * Listado admin filtrable por estado y/o trabajador (ambos opcionales). {@code null} en un
     * parámetro = sin filtrar por ese campo.
     */
    @Query("SELECT b FROM Booking b LEFT JOIN b.staff s " +
           "WHERE (:status IS NULL OR b.status = :status) " +
           "AND (:staffId IS NULL OR s.id = :staffId) " +
           "ORDER BY b.date DESC, b.time ASC")
    Page<Booking> search(@Param("status") BookingStatus status,
                         @Param("staffId") String staffId,
                         Pageable pageable);

    /** Reservas de un cliente por email. */
    List<Booking> findByClientEmailOrderByDateDescTimeAsc(String email);

    /**
     * Reservas activas (no canceladas) en un rango de fechas — para el calendario público.
     * {@code JOIN FETCH b.service} trae el servicio en la misma consulta (evita el N+1 al
     * calcular la hora de fin con la duración del servicio).
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.service " +
           "WHERE b.date BETWEEN :from AND :to AND b.status <> 'CANCELLED' " +
           "ORDER BY b.date ASC, b.time ASC")
    List<Booking> findBusyBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
