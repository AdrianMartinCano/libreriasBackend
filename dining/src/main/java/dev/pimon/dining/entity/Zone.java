package dev.pimon.dining.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Zona física del local (Terraza, Salón, Barra...). Entidad —no enum— porque cada negocio define las
 * suyas. Las combinaciones de mesas solo se permiten dentro de la misma zona.
 */
@Entity
@Table(name = "dining_zones")
@Getter
@Setter
@NoArgsConstructor
@BatchSize(size = 20)
public class Zone extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    public Zone(String name, int sortOrder) {
        this.name      = name;
        this.sortOrder = sortOrder;
    }
}
