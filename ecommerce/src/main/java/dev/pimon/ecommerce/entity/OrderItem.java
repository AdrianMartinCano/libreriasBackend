package dev.pimon.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

import java.math.BigDecimal;

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

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;        // precio en el momento de la compra

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Lo que el cliente eligió para esta línea, serializado como JSON.
     *
     * SE GUARDA COMO TEXTO Y NO EN UNA TABLA APARTE a propósito. Es un dato de SOLO
     * LECTURA una vez hecho el pedido —igual que el nombre y el precio de al lado— y nadie
     * va a consultar pedidos filtrando por color. Una tabla de opciones traería un join en
     * cada consulta de pedidos para no ganar nada.
     *
     * Y por el mismo motivo que el resto de la fila: es una FOTO del momento de la compra.
     * Si mañana se retira el color "Morado", este pedido tiene que seguir diciendo que se
     * pidió morado.
     */
    @Column(columnDefinition = "TEXT")
    private String opciones;

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
