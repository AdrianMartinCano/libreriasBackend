package dev.pimon.reservations.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "reservation_services")
@Getter
@Setter
@NoArgsConstructor
public class ServiceOffering extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int durationMinutes;

    private BigDecimal price;

    private String imageUrl;

    /** Color hex para el badge en el calendario (ej. "#1e3a8a"). */
    private String color;

    private boolean active = true;

    public ServiceOffering(String name, String description, int durationMinutes,
                           BigDecimal price, String imageUrl, String color) {
        this.name            = name;
        this.description     = description;
        this.durationMinutes = durationMinutes;
        this.price           = price;
        this.imageUrl        = imageUrl;
        this.color           = color;
    }
}
