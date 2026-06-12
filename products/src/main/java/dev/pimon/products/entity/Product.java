package dev.pimon.products.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    private Double originalPrice;   // null = sin descuento

    @Column(nullable = false)
    private Integer stock = 0;

    private String imageUrl;
    private String badge;           // "Nuevo", "-20%", "Oferta"...
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public boolean isInStock() {
        return stock != null && stock > 0;
    }

    public Integer getDiscount() {
        if (originalPrice == null || originalPrice <= price) return null;
        return (int) Math.round((1 - price / originalPrice) * 100);
    }
}
