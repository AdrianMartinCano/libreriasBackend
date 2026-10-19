package dev.pimon.dining.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reserva de mesa. El eje es {@code partySize} (comensales) y las {@code tables} asignadas (varias =
 * mesas juntadas). El solape se evalúa por mesa: una mesa no puede estar en dos reservas activas con
 * turnos solapados el mismo día.
 */
@Entity
@Table(name = "dining_reservations", indexes = {
        @Index(name = "idx_dres_date", columnList = "date"),
        @Index(name = "idx_dres_client_email", columnList = "clientEmail")
})
@Getter
@Setter
@NoArgsConstructor
public class DiningReservation extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seating_id", nullable = false)
    private Seating seating;

    @Column(nullable = false)
    private int partySize;

    /** Mesa(s) asignada(s). 1 = mesa simple; 2+ = combinación de mesas juntadas. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "dining_reservation_tables",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "table_id"),
            indexes = @Index(name = "idx_drt_table", columnList = "table_id"))
    private Set<DiningTable> tables = new LinkedHashSet<>();

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String clientEmail;

    @Column(nullable = false)
    private String clientPhone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dining_reservation_requests", joinColumns = @JoinColumn(name = "reservation_id"))
    @Column(name = "request", length = 20)
    @Enumerated(EnumType.STRING)
    private Set<SpecialRequest> requests = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    /** Depósito/señal cobrado (null = sin depósito). Integración de pago: fase posterior. */
    @Column(precision = 12, scale = 2)
    private BigDecimal deposit;

    /** ¿Cuenta esta reserva como ocupación activa (mesa y aforo tomados)? */
    public boolean isActive() {
        return status == ReservationStatus.PENDING
                || status == ReservationStatus.CONFIRMED
                || status == ReservationStatus.SEATED;
    }
}
