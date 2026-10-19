package dev.pimon.reservations.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Trabajador/recurso con su propia agenda (profesional de la peluquería, tatuador, sillón, sala...).
 *
 * <p><b>Login opcional y desacoplado:</b> {@code userId} guarda el id del usuario autenticado
 * ({@code AuthUser.id}) como String <b>sin relación JPA ni FK</b>, para no acoplar este módulo al de
 * usuarios. {@code null} = trabajador solo-recurso (sin cuenta; lo gestiona el jefe). La existencia del
 * usuario se valida en la capa de servicio, no en BD.</p>
 *
 * <p><b>{@code @BatchSize}</b> evita el N+1 al listar reservas con su trabajador asociado.</p>
 */
@Entity
@Table(name = "staff_members", indexes = {
        @Index(name = "idx_staff_active", columnList = "active"),
        @Index(name = "idx_staff_user",   columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@BatchSize(size = 50)
public class StaffMember extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Color hex para su columna y sus eventos en el calendario (ej. "#1e3a8a"). */
    private String color;

    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    /** Orden de la columna en la vista de día del jefe (menor primero). */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /**
     * Id del {@code AppUser} vinculado (login del trabajador). {@code null} = sin cuenta. Es el
     * {@code AuthUser.id}; String suelto para no acoplar reservations→users (ver javadoc de la clase).
     */
    @Column(name = "user_id", unique = true)
    private String userId;

    /**
     * Si es {@code true}, usa su propio {@link #workSlots}; si es {@code false}, hereda
     * {@code ReservationsSettings.weeklyHours} del negocio.
     */
    @Column(name = "custom_hours", nullable = false)
    private boolean customHours = false;

    /** Horario propio (tramos por día). Solo se aplica si {@link #customHours} es {@code true}. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "staff_work_slots", joinColumns = @JoinColumn(name = "staff_id"))
    @BatchSize(size = 50)
    private List<WorkSlot> workSlots = new ArrayList<>();

    /**
     * Vacaciones/ausencias propias en formato ISO 'YYYY-MM-DD'. Regla <b>aditiva</b>: cierran solo a
     * este trabajador y se suman a los festivos del negocio ({@code ReservationsSettings.blockedDates}),
     * que cierran a todos.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "staff_blocked_dates", joinColumns = @JoinColumn(name = "staff_id"))
    @Column(name = "blocked_date", nullable = false)
    @BatchSize(size = 50)
    private Set<LocalDate> blockedDates = new HashSet<>();

    /**
     * Servicios que realiza este trabajador. <b>Vacío = hace todos</b> (caso peluquería); no vacío =
     * solo esos (caso tatuador con especialidades).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "staff_services",
            joinColumns        = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    @BatchSize(size = 50)
    private Set<ServiceOffering> services = new HashSet<>();

    /** true si este trabajador puede realizar el servicio dado (vacío = todos). */
    public boolean canPerform(ServiceOffering service) {
        return services.isEmpty() || services.contains(service);
    }
}
