package dev.pimon.events.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Confirmación de asistencia (RSVP) de un invitado a un evento.
 * Se agrupa por {@code eventId} (slug del evento en la web).
 */
@Entity
@Table(name = "events_rsvp", indexes = @Index(name = "idx_rsvp_event", columnList = "eventId"))
@Getter
@Setter
@NoArgsConstructor
public class Rsvp extends BaseEntity {

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String name;

    private String email;

    @Column(nullable = false)
    private boolean attending;

    /** Acompañantes además del propio invitado (0 = va solo). */
    @Column(nullable = false)
    private int guests;

    private String menuChoice;

    @Column(length = 500)
    private String allergies;

    private String song;

    @Column(length = 1000)
    private String message;
}
