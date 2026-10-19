package dev.pimon.reservations.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = "bookings", indexes = {
        // date: disponibilidad por día y rango "busy"; clientEmail: reservas del cliente.
        @Index(name = "idx_booking_date", columnList = "date"),
        @Index(name = "idx_booking_client_email", columnList = "clientEmail"),
        // staff_id + date: agenda de un trabajador en un día (timeline y solape por persona).
        @Index(name = "idx_booking_staff_date", columnList = "staff_id, date")
})
@Getter
@Setter
@NoArgsConstructor
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    /**
     * Trabajador asignado a la cita. {@code nullable} por compatibilidad con datos previos al
     * multistaff (se rellenan con el trabajador por defecto en el backfill) y para citas "sin asignar".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private StaffMember staff;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 5)
    private String time;  // "10:00"

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String clientEmail;

    @Column(nullable = false)
    private String clientPhone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Duración real en minutos (alta admin de X horas). Si es null, se usa la del servicio. */
    @Column
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }
}
