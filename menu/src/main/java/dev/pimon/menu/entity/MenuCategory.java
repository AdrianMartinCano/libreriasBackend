package dev.pimon.menu.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Categoría de la carta (Entrantes, Carnes, Postres, Vinos...). Permanente: no cambia a diario.
 * Decoración solo por {@link CategoryIcon} curado — sin imágenes subidas.
 */
@Entity
@Table(name = "menu_categories")
@Getter
@Setter
@NoArgsConstructor
@BatchSize(size = 50)
public class MenuCategory extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryIcon icon = CategoryIcon.NONE;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    public MenuCategory(String name, String slug, String description, CategoryIcon icon, int sortOrder) {
        this.name        = name;
        this.slug        = slug;
        this.description = description;
        this.icon        = icon != null ? icon : CategoryIcon.NONE;
        this.sortOrder   = sortOrder;
    }
}
