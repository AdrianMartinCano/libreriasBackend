package dev.pimon.products.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;
import org.hibernate.annotations.BatchSize;

/**
 * {@code @BatchSize} agrupa la inicialización de los proxies LAZY de Category: al listar una
 * página de productos, en vez de una consulta de categoría por producto (N+1), Hibernate carga
 * las categorías en lotes de 50 con un único "... IN (?, ?, ...)".
 */
@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
@BatchSize(size = 50)
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;
    private String imageUrl;
    private boolean active = true;

    public Category(String name, String slug, String description, String imageUrl) {
        this.name        = name;
        this.slug        = slug;
        this.description = description;
        this.imageUrl    = imageUrl;
    }
}
