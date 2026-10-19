package dev.pimon.dining.repository;

import dev.pimon.dining.entity.DiningReservation;
import dev.pimon.dining.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DiningReservationRepository extends JpaRepository<DiningReservation, String> {

    /**
     * Reservas activas de un día con turno y mesas cargados (para calcular ocupación y solape por
     * mesa sin N+1). Solo los estados que ocupan: PENDING/CONFIRMED/SEATED.
     */
    @Query("SELECT DISTINCT r FROM DiningReservation r " +
           "JOIN FETCH r.seating " +
           "LEFT JOIN FETCH r.tables " +
           "WHERE r.date = :date AND r.status IN ('PENDING','CONFIRMED','SEATED')")
    List<DiningReservation> findActiveByDateWithTables(@Param("date") LocalDate date);

    /** Reservas de un día (cualquier estado), con turno y mesas — alimenta el plano de sala admin. */
    @Query("SELECT DISTINCT r FROM DiningReservation r " +
           "JOIN FETCH r.seating " +
           "LEFT JOIN FETCH r.tables " +
           "WHERE r.date = :date ORDER BY r.createdAt ASC")
    List<DiningReservation> findByDateWithTables(@Param("date") LocalDate date);

    /** Listado admin filtrable por estado (opcional), más recientes primero. */
    @Query("SELECT r FROM DiningReservation r " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "ORDER BY r.date DESC, r.createdAt DESC")
    Page<DiningReservation> search(@Param("status") ReservationStatus status, Pageable pageable);

    List<DiningReservation> findByClientEmailOrderByDateDesc(String email);

    /** ¿Hay alguna reserva que use esta mesa? — impide borrar la mesa (debe desactivarse). */
    boolean existsByTables_Id(String tableId);

    /** ¿Hay alguna reserva con este turno? — impide borrar el turno. */
    boolean existsBySeating_Id(String seatingId);
}
