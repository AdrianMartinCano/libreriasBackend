package dev.pimon.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

/**
 * Ítem de un pedido — datos desnormalizados del producto en el momento de la compra.
 * Se guardan aunque el producto original cambie de precio o se elimine.
 */
@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String productName;

    private String productImageUrl;

    @Column(nullable = false)
    private Double unitPrice;        // precio en el momento de la compra

    @Column(nullable = false)
    private Integer quantity;

    public double subtotal() {
        return unitPrice * quantity;
    }
}
