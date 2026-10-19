package dev.pimon.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        // myOrders() filtra por userId; el webhook busca por stripe_payment_intent_id en cada evento.
        @Index(name = "idx_order_user", columnList = "userId"),
        @Index(name = "idx_order_payment_intent", columnList = "stripePaymentIntentId")
})
@Getter @Setter @NoArgsConstructor
public class Order extends BaseEntity {

    @Column(nullable = false)
    private String userId;      // id del usuario que compra

    @Column(nullable = false)
    private String userEmail;

    // @BatchSize evita el N+1 al listar pedidos: carga los ítems de varios pedidos
    // en pocas consultas "... IN (?, ?, ...)" en lugar de una por pedido.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    /** Gastos de envío incluidos en el total. */
    @Column(precision = 12, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    /** Total cobrado = subtotal de artículos + envío. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    // ── Stripe ─────────────────────────────────────────────────
    private String stripePaymentIntentId;   // pi_xxxxx
    private String stripeClientSecret;       // pi_xxxxx_secret_yyyyy (para el frontend)

    // ── Envío ──────────────────────────────────────────────────
    private String shippingName;
    private String shippingAddress;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;
    private String shippingPhone;
}
