package dev.pimon.ecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.ecommerce.dto.*;
import dev.pimon.ecommerce.entity.Order;
import dev.pimon.ecommerce.entity.OrderItem;
import dev.pimon.ecommerce.entity.OrderStatus;
import dev.pimon.ecommerce.repository.OrderRepository;
import dev.pimon.products.entity.Product;
import dev.pimon.products.repository.ProductRepository;
import dev.pimon.security.model.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository   orderRepo;
    private final ProductRepository productRepo;
    private final StripeService     stripeService;

    // ── Checkout ─────────────────────────────────────────────────────────

    /**
     * Crea una orden y un PaymentIntent de Stripe.
     * Devuelve el clientSecret para que el frontend muestre el formulario de pago.
     */
    public CheckoutResponse createCheckoutIntent(CheckoutRequest req, AuthUser user) {
        // 1. Validar productos y calcular total
        List<OrderItem> items = buildItems(req.items());
        double total = items.stream().mapToDouble(OrderItem::subtotal).sum();

        // 2. Crear la orden en BD (estado PENDING_PAYMENT)
        Order order = new Order();
        order.setUserId(user.id());
        order.setUserEmail(user.email());
        order.setTotal(total);
        order.setShippingName(req.shippingName());
        order.setShippingAddress(req.shippingAddress());
        order.setShippingCity(req.shippingCity());
        order.setShippingPostalCode(req.shippingPostalCode());
        order.setShippingCountry(req.shippingCountry());
        order.setShippingPhone(req.shippingPhone());

        order = orderRepo.save(order);  // necesitamos el id para el PaymentIntent

        // Relacionar items con la orden
        final Order savedOrder = order;
        items.forEach(item -> item.setOrder(savedOrder));
        order.setItems(items);

        // 3. Crear PaymentIntent en Stripe (o simulado)
        long amountInCents = Math.round(total * 100);
        String clientSecret = stripeService.createPaymentIntent(amountInCents, order.getId());

        order.setStripeClientSecret(clientSecret);
        order.setStripePaymentIntentId(stripeService.extractPaymentIntentId(clientSecret));
        orderRepo.save(order);

        log.info("Checkout iniciado: orden {} — total {}€ — usuario {}",
                 order.getId(), total, user.email());

        return new CheckoutResponse(order.getId(), clientSecret, total, "EUR");
    }

    // ── Webhook (llamado por Stripe) ──────────────────────────────────────

    /** Marca la orden como PAID cuando Stripe confirma el pago */
    public void handlePaymentSuccess(String paymentIntentId) {
        orderRepo.findByStripePaymentIntentId(paymentIntentId).ifPresentOrElse(
                order -> {
                    order.setStatus(OrderStatus.PAID);
                    orderRepo.save(order);
                    log.info("Orden {} marcada como PAID (PaymentIntent: {})",
                             order.getId(), paymentIntentId);
                },
                () -> log.warn("Webhook: orden no encontrada para PaymentIntent {}", paymentIntentId)
        );
    }

    /** Marca la orden como CANCELLED si el pago falla */
    public void handlePaymentFailure(String paymentIntentId) {
        orderRepo.findByStripePaymentIntentId(paymentIntentId).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepo.save(order);
            log.info("Orden {} cancelada por fallo de pago (PaymentIntent: {})",
                     order.getId(), paymentIntentId);
        });
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> myOrders(String userId, Pageable pageable) {
        return PageResponse.from(
                orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto)
        );
    }

    @Transactional(readOnly = true)
    public OrderDto getMyOrder(String orderId, String userId) {
        Order order = findById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw AppException.forbidden("No tienes acceso a este pedido");
        }
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> listAll(Pageable pageable) {
        return PageResponse.from(orderRepo.findAll(pageable).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public OrderDto getById(String id) {
        return toDto(findById(id));
    }

    public OrderDto updateStatus(String id, OrderStatus status) {
        Order order = findById(id);
        order.setStatus(status);
        return toDto(orderRepo.save(order));
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private List<OrderItem> buildItems(List<CheckoutItemRequest> requests) {
        return requests.stream().map(req -> {
            Product product = productRepo.findById(req.productId())
                    .filter(p -> p.isActive() && p.isInStock())
                    .orElseThrow(() -> AppException.badRequest(
                            "Producto no disponible: " + req.productId()));

            if (product.getStock() < req.quantity()) {
                throw AppException.badRequest(
                        "Stock insuficiente para: " + product.getName()
                        + " (disponible: " + product.getStock() + ")");
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImageUrl(product.getImageUrl());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(req.quantity());

            // Decrementar stock
            product.setStock(product.getStock() - req.quantity());
            productRepo.save(product);

            return item;
        }).toList();
    }

    private Order findById(String id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Pedido no encontrado: " + id));
    }

    private OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream().map(i ->
                new OrderItemDto(i.getProductId(), i.getProductName(), i.getProductImageUrl(),
                                 i.getUnitPrice(), i.getQuantity(), i.subtotal())
        ).toList();

        return new OrderDto(o.getId(), o.getUserId(), o.getUserEmail(), items, o.getTotal(),
                o.getStatus(), o.getShippingName(), o.getShippingAddress(),
                o.getShippingCity(), o.getShippingCountry(), o.getCreatedAt());
    }
}
