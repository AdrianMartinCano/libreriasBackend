package dev.pimon.ecommerce.repository;

import dev.pimon.ecommerce.entity.Order;
import dev.pimon.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<Order> findByStripePaymentIntentId(String paymentIntentId);

    /**
     * Los pedidos que llevan demasiado tiempo esperando un pago que no llega.
     *
     * Se filtra por FECHA DE CREACION y no por la de modificacion: un pedido pendiente no
     * se toca mientras espera, asi que son la misma, y createdAt no depende de que nadie
     * se acuerde de actualizarla.
     */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime limite);
}
