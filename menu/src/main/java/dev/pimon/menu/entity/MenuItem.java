package dev.pimon.menu.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Plato de la carta. Sin imagen (carta tipográfica): solo texto, precio(s), alérgenos y flags
 * dietéticos. {@code available=false} = "agotado hoy" sin borrarlo.
 */
@Entity
@Table(name = "menu_items", indexes = {
        // El listado público filtra por categoría y disponibilidad.
        @Index(name = "idx_menuitem_category", columnList = "category_id"),
        @Index(name = "idx_menuitem_available", columnList = "available")
})
@Getter
@Setter
@NoArgsConstructor
public class MenuItem extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Precio base / única ración. Si {@link #portions} no está vacío, manda la lista de raciones y
     * este campo actúa como "desde" (puede ser null cuando solo hay raciones).
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** Raciones con precio (tapa/media/ración). Vacío = se usa {@link #price}. */
    @ElementCollection
    @CollectionTable(name = "menu_item_portions", joinColumns = @JoinColumn(name = "item_id"))
    @OrderColumn(name = "sort_order")
    private List<PricedPortion> portions = new java.util.ArrayList<>();

    /** Alérgenos presentes (los 14 de declaración obligatoria UE). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "menu_item_allergens", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "allergen", length = 20)
    @Enumerated(EnumType.STRING)
    private Set<Allergen> allergens = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean vegan;

    @Column(nullable = false)
    private boolean vegetarian;

    @Column(nullable = false)
    private boolean glutenFree;

    @Column(nullable = false)
    private boolean spicy;

    /** Distintivo textual: "Sugerencia del chef", "Nuevo"... (no es imagen). */
    private String badge;

    /** Disponible hoy. Permite "agotar" sin borrar el plato de la carta. */
    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;
}
