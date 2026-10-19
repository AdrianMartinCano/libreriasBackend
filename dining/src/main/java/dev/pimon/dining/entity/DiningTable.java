package dev.pimon.dining.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Mesa del local. La capacidad se mide en <b>comensales</b> (pax), no en tiempo: ese es el eje del
 * dominio de restauración. {@code combinable} permite juntarla con otras de su zona para grupos
 * grandes. {@code posX/posY} son opcionales para el plano de sala.
 */
@Entity
@Table(name = "dining_tables",
        indexes = @Index(name = "idx_table_zone", columnList = "zone_id"))
@Getter
@Setter
@NoArgsConstructor
@BatchSize(size = 50)
public class DiningTable extends BaseEntity {

    @Column(nullable = false)
    private String label;   // "Mesa 4", "Barra 2"

    /** Mínimo razonable de pax (evita sentar 2 en una mesa de 8). */
    @Column(nullable = false)
    private int capacityMin = 1;

    /** Tope de pax de la mesa. */
    @Column(nullable = false)
    private int capacityMax;

    /** Se puede unir a otras mesas combinables de su misma zona para formar un grupo mayor. */
    @Column(nullable = false)
    private boolean combinable;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    private Integer posX;
    private Integer posY;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    /** ¿Esta mesa sola admite un grupo de {@code party} comensales? */
    public boolean fits(int party) {
        return party >= capacityMin && party <= capacityMax;
    }
}
