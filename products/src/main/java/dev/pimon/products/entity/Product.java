package dev.pimon.products.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "products", indexes = {
        // El listado filtra siempre por active y, opcionalmente, por precio.
        // La categoría ya no se filtra por aquí: vive en product_categories, que lleva
        // su propio índice sobre category_id.
        @Index(name = "idx_product_active", columnList = "active")
})
@Getter @Setter @NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Dinero en BigDecimal (precision/scale fijos) — evita los errores de redondeo de Double.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal originalPrice;   // null = sin descuento

    @Column(nullable = false)
    private Integer stock = 0;

    private String imageUrl;
    private String badge;           // "Nuevo", "-20%", "Oferta"...
    private boolean active = true;

    /**
     * Las categorías del producto. <b>Varias</b>, desde la 0.10.0.
     *
     * <h2>Por qué dejó de ser una</h2>
     *
     * Porque un producto pertenece de verdad a más de un sitio a la vez, y con una sola
     * había que elegir: un llavero de Halloween es «Coleccionismo» siempre y «Halloween»
     * durante dos meses. Con {@code @ManyToOne}, meterlo en la campaña lo sacaba de su
     * categoría de siempre — y al acabar la temporada había que acordarse de devolverlo.
     *
     * <h2>@BatchSize va AQUÍ, y no basta con el de Category</h2>
     *
     * {@link Category} ya lleva el suyo, pero ese agrupa la carga de <i>proxies de
     * entidad</i>; esto es una <b>colección</b>, y las colecciones se agrupan con el
     * {@code @BatchSize} de la propia colección. Sin él, listar 24 productos son 24
     * consultas extra —una por producto— y el N+1 no se nota en local con cinco piezas:
     * se nota en la tienda llena.
     *
     * <h2>El orden es por nombre</h2>
     *
     * Para que la ficha y la tarjeta pinten siempre las categorías en el mismo orden. Un
     * {@code Set} sin orden las pinta como salga de la base, que puede cambiar entre dos
     * peticiones iguales y se lee como un fallo.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"),
            indexes = @Index(name = "idx_product_categories_category", columnList = "category_id")
    )
    @BatchSize(size = 50)
    @OrderBy("name")
    private Set<Category> categories = new LinkedHashSet<>();

    public boolean isInStock() {
        return stock != null && stock > 0;
    }

    public Integer getDiscount() {
        if (originalPrice == null || price == null || originalPrice.compareTo(price) <= 0) return null;
        // % = (1 - price/originalPrice) * 100, redondeado al entero
        BigDecimal ratio = price.divide(originalPrice, 4, RoundingMode.HALF_UP);
        return BigDecimal.ONE.subtract(ratio)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
