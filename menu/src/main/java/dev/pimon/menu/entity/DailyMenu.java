package dev.pimon.menu.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Menú del día: oferta a precio fijo para una fecha concreta (primeros, segundos, postre...).
 * Convive con la carta (agregados independientes). Se puede tener más de uno por día —del día,
 * ejecutivo, fin de semana— de ahí la clave única (date, name). {@code published} separa el
 * borrador del menú visible en la web.
 */
@Entity
@Table(name = "menu_daily",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_date_name", columnNames = {"date", "name"}),
        indexes = @Index(name = "idx_daily_date", columnList = "date"))
@Getter
@Setter
@NoArgsConstructor
public class DailyMenu extends BaseEntity {

    @Column(nullable = false)
    private LocalDate date;

    /** Nombre del menú: "Menú del día", "Menú ejecutivo"... */
    @Column(nullable = false)
    private String name = "Menú del día";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Texto libre: "Incluye pan, bebida y postre o café". */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean published;

    private LocalDateTime publishedAt;

    /** Grupos de elección (Primeros, Segundos, Postres). Orden estable por {@code @OrderColumn}. */
    @OneToMany(mappedBy = "dailyMenu", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "sort_order")
    private List<DailyMenuCourse> courses = new ArrayList<>();

    /** Añade un grupo manteniendo la relación bidireccional. */
    public void addCourse(DailyMenuCourse course) {
        course.setDailyMenu(this);
        this.courses.add(course);
    }
}
