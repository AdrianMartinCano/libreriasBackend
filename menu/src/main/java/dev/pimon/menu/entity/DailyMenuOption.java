package dev.pimon.menu.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Una opción de un {@link DailyMenuCourse}. Dos formas:
 * <ul>
 *   <li><b>Reutiliza un plato de la carta</b> ({@link #item} != null): hereda nombre, descripción y
 *       alérgenos del {@link MenuItem} (un cambio en la carta se propaga).</li>
 *   <li><b>Línea libre</b> ({@link #item} == null): el especial del día, con su propio nombre,
 *       descripción y alérgenos.</li>
 * </ul>
 */
@Entity
@Table(name = "menu_daily_option")
@Getter
@Setter
@NoArgsConstructor
public class DailyMenuOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private DailyMenuCourse course;

    /** Plato de la carta reutilizado. Null = línea libre (usa los campos de abajo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private MenuItem item;

    /** Nombre cuando es línea libre (ignorado si {@link #item} != null). */
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Suplemento sobre el precio fijo del menú (p. ej. +3,00€ solomillo). Null = sin suplemento. */
    @Column(precision = 12, scale = 2)
    private BigDecimal supplement;

    /** Alérgenos cuando es línea libre. Si {@link #item} != null, se usan los del plato. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "menu_daily_option_allergens", joinColumns = @JoinColumn(name = "option_id"))
    @Column(name = "allergen", length = 20)
    @Enumerated(EnumType.STRING)
    private Set<Allergen> allergens = new LinkedHashSet<>();
}
