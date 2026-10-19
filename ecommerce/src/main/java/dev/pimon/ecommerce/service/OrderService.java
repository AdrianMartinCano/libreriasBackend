package dev.pimon.ecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.ecommerce.dto.*;
import dev.pimon.ecommerce.event.PedidoCambioEstado;
import dev.pimon.ecommerce.entity.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pimon.ecommerce.entity.OrderItem;
import dev.pimon.ecommerce.entity.OrderStatus;
import dev.pimon.ecommerce.repository.OrderRepository;
import dev.pimon.products.entity.Product;
import dev.pimon.products.repository.ProductRepository;
import dev.pimon.security.model.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository    orderRepo;
    private final ProductRepository  productRepo;
    private final StripeService      stripeService;
    private final StockDePedidos     stock;
    private final ApplicationEventPublisher eventos;
    private final dev.pimon.ecommerce.config.EcommerceProperties props;
    private final CalculadorEnvio    calculadorEnvio;
    private final ValidadorOpciones  validadorOpciones;
    private final ObjectMapper       objectMapper;

    // ── Checkout ─────────────────────────────────────────────────────────

    /**
     * Crea una orden y un PaymentIntent de Stripe.
     * Devuelve el clientSecret para que el frontend muestre el formulario de pago.
     */
    public CheckoutResponse createCheckoutIntent(CheckoutRequest req, AuthUser user) {
        // 1. Validar productos y calcular subtotal (suma exacta en BigDecimal)
        List<OrderItem> items = buildItems(req.items());
        BigDecimal subtotal = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 1b. Gastos de envío. Lo calcula el CalculadorEnvio que haya registrado el
        // proyecto; si no hay ninguno, EnvioTarifaPlana aplica el coste fijo de siempre.
        // Puede lanzar (zona no servida, peso fuera de tarifa) y entonces el checkout se
        // aborta antes de crear nada, que es lo correcto: mejor no aceptar un pedido que
        // aceptarlo sin saber qué cuesta mandarlo.
        BigDecimal shipping = calculadorEnvio.calcular(items, req, subtotal);
        BigDecimal total = subtotal.add(shipping).setScale(2, RoundingMode.HALF_UP);

        // 2. Crear la orden en BD (estado PENDING_PAYMENT)
        Order order = new Order();
        order.setUserId(user.id());
        order.setUserEmail(user.email());
        order.setShippingCost(shipping);
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

        // 3. Crear PaymentIntent en Stripe (o simulado). Stripe trabaja en céntimos enteros.
        long amountInCents = total.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String clientSecret = stripeService.createPaymentIntent(amountInCents, order.getId());

        order.setStripeClientSecret(clientSecret);
        order.setStripePaymentIntentId(stripeService.extractPaymentIntentId(clientSecret));
        orderRepo.save(order);

        log.info("Checkout iniciado: orden {} — subtotal {}€ + envío {}€ = {}€ — usuario {}",
                 order.getId(), subtotal, shipping, total, user.email());

        return new CheckoutResponse(order.getId(), clientSecret, subtotal, shipping, total, "EUR");
    }

    // ── Webhook (llamado por Stripe) ──────────────────────────────────────

    /**
     * Marca la orden como PAID cuando Stripe confirma el pago.
     * <b>Idempotente</b>: Stripe puede reentregar el webhook varias veces; solo transiciona
     * desde PENDING_PAYMENT, de modo que un reenvío no revierte un pedido ya avanzado
     * (PROCESSING/SHIPPED/...).
     */
    public void handlePaymentSuccess(String paymentIntentId) {
        orderRepo.findByStripePaymentIntentId(paymentIntentId).ifPresentOrElse(
                order -> {
                    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                        log.info("Webhook duplicado/tardío ignorado: orden {} ya está en {}",
                                 order.getId(), order.getStatus());
                        return;
                    }
                    order.setStatus(OrderStatus.PAID);
                    anunciar(orderRepo.save(order), OrderStatus.PENDING_PAYMENT);
                    log.info("Orden {} marcada como PAID (PaymentIntent: {})",
                             order.getId(), paymentIntentId);
                },
                () -> log.warn("Webhook: orden no encontrada para PaymentIntent {}", paymentIntentId)
        );
    }

    /**
     * Marca la orden como CANCELLED si el pago falla.
     * Solo cancela pedidos aún PENDING_PAYMENT: nunca cancela uno ya pagado (evita que un
     * evento de fallo tardío anule un pago confirmado).
     */
    public void handlePaymentFailure(String paymentIntentId) {
        orderRepo.findByStripePaymentIntentId(paymentIntentId).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                log.info("Fallo de pago ignorado: orden {} ya está en {}",
                         order.getId(), order.getStatus());
                return;
            }
            // Se devuelve el stock ANTES de cancelar. Sin esto, el pedido quedaba
            // cancelado con sus unidades restadas y ya nadie las recuperaba: la caducidad
            // solo mira los que siguen en PENDING_PAYMENT, y este ya no lo esta.
            stock.devolver(order);
            order.setStatus(OrderStatus.CANCELLED);
            anunciar(orderRepo.save(order), OrderStatus.PENDING_PAYMENT);
            log.info("Orden {} cancelada por fallo de pago y devuelto su stock "
                     + "(PaymentIntent: {})", order.getId(), paymentIntentId);
        });
    }

    /**
     * Cancela un pedido que caduco sin pagarse: devuelve su stock y lo anuncia.
     *
     * Lo llama {@link CaducidadPedidos}, que decide CUALES caducan —cuanto tiempo, si hay
     * dinero de por medio— pero no toca el pedido: cambiar de estado es cosa de aqui, y
     * repartirlo entre dos clases es como se acaba publicando el evento en tres caminos y
     * olvidandolo en el cuarto.
     */
    void cancelarPorCaducidad(Order order) {
        stock.devolver(order);
        order.setStatus(OrderStatus.CANCELLED);
        anunciar(orderRepo.save(order), OrderStatus.PENDING_PAYMENT);
    }

    /**
     * Cuenta que el pedido ha cambiado de estado, y devuelve su DTO.
     *
     * Un solo sitio publica, para que no se olvide en uno de los cuatro caminos. Que
     * devuelva el DTO no es casualidad: asi el uso natural es
     * {@code return anunciar(save(order), antes)} y cuesta mas guardar sin anunciar.
     */
    private OrderDto anunciar(Order guardada, OrderStatus anterior) {
        OrderDto dto = toDto(guardada);
        eventos.publishEvent(new PedidoCambioEstado(dto, anterior));
        return dto;
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

    /**
     * Cambia el estado de un pedido.
     *
     * PASAR A REFUNDED DEVUELVE EL DINERO DE VERDAD. Hasta ahora este método solo cambiaba
     * la etiqueta, así que el panel decía "Reembolsado" —y la tienda mandaba el correo
     * correspondiente— mientras el dinero seguía en la cuenta. Un pedido que miente sobre
     * el dinero de alguien es peor que no tener el botón.
     *
     * EL ORDEN IMPORTA Y NO SE PUEDE INVERTIR: primero se llama a Stripe y solo si
     * responde bien se guarda el estado. Si Stripe falla, la excepción sale de aquí y el
     * pedido se queda como estaba, que es lo correcto: el dinero no ha salido.
     *
     * Repetir la operación sobre un pedido ya reembolsado no hace nada y no es un error.
     * Es deliberado: el panel puede pulsarse dos veces, y para quien mira el resultado
     * "está reembolsado" ya es cierto.
     */
    public OrderDto updateStatus(String id, OrderStatus status) {
        Order order = findById(id);
        OrderStatus actual = order.getStatus();

        // Pedir el estado que ya tiene no es un error: el panel puede estar abierto en dos
        // pestañas, o alguien pulsa dos veces. Para quien mira el resultado, ya es cierto.
        if (actual == status) {
            log.info("La orden {} ya está en {}; no se hace nada", id, status);
            return toDto(order);
        }

        // LA VALIDACIÓN VA ANTES DE TOCAR STRIPE. Si no, un cambio prohibido que además
        // fuera un reembolso habría movido el dinero antes de rechazarse.
        if (!actual.puedePasarA(status)) {
            throw AppException.badRequest(mensajeDeTransicionInvalida(actual, status));
        }

        if (status == OrderStatus.REFUNDED) {
            return toDto(reembolsar(order));
        }

        // Cancelar devuelve las unidades al catalogo. Solo se llega a CANCELLED desde
        // PENDING_PAYMENT —lo impone puedePasarA— asi que el stock siempre esta restado y
        // nunca se suma dos veces.
        //
        // REFUNDED no lo hace, y es a proposito: ahi el articulo ya salio por la puerta.
        // Que vuelva al catalogo es una decision del negocio —un libro se revende, una
        // pieza impresa a medida no— y por eso no se decide aqui.
        if (status == OrderStatus.CANCELLED) {
            stock.devolver(order);
            log.info("Orden {} cancelada: devuelto su stock al catalogo", order.getId());
        }

        order.setStatus(status);
        return anunciar(orderRepo.save(order), actual);
    }

    /**
     * Explica por qué no se puede hacer ese cambio, en lugar de un "transición inválida".
     *
     * Quien lee esto está delante del panel intentando arreglar un pedido de alguien, y
     * necesita saber qué hacer, no qué ha fallado.
     */
    private String mensajeDeTransicionInvalida(OrderStatus actual, OrderStatus pedido) {
        if (actual.esFinal()) {
            return "El pedido está en " + actual + ", que es un estado final: ya no admite cambios.";
        }
        if (pedido == OrderStatus.CANCELLED) {
            return "Este pedido ya está cobrado, así que no se puede cancelar: usa REFUNDED, "
                   + "que además devuelve el dinero en Stripe.";
        }
        return "No se puede pasar de " + actual + " a " + pedido
               + ". Desde " + actual + " solo se puede ir a: " + actual.siguientesPosibles() + ".";
    }

    /**
     * Devuelve el dinero en Stripe y, solo entonces, marca el pedido como reembolsado.
     *
     * No comprueba de qué estado viene ni si ya estaba reembolsado: de eso se encarga
     * {@link OrderStatus#puedePasarA} antes de llegar aquí. Repetir esas reglas en dos
     * sitios sería peor que no tenerlas, porque el día que cambie una la otra se queda
     * desfasada en silencio.
     */
    private Order reembolsar(Order order) {
        long amountInCents = order.getTotal()
                .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();

        // Si esto lanza, no se llega al save: el estado no cambia y el dinero tampoco.
        stripeService.refund(order.getStripePaymentIntentId(), amountInCents);

        OrderStatus antes = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED);
        Order guardada = orderRepo.save(order);
        anunciar(guardada, antes);

        log.info("Orden {} reembolsada: {}€ devueltos a {}",
                 order.getId(), order.getTotal(), order.getUserEmail());

        return guardada;
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private List<OrderItem> buildItems(List<CheckoutItemRequest> requests) {
        return requests.stream().map(req -> {
            Product product = productRepo.findById(req.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> AppException.badRequest(
                            "Producto no disponible: " + req.productId()));

            // Descuento ATÓMICO: comprueba y resta el stock en una sola sentencia SQL.
            // Si devuelve 0 no había stock suficiente (cubre también la carrera entre dos
            // checkouts simultáneos del último ítem → imposible sobrevender).
            int updated = productRepo.decrementStock(product.getId(), req.quantity());
            if (updated == 0) {
                throw AppException.badRequest("Stock insuficiente para: " + product.getName());
            }

            // Las opciones se comprueban SIEMPRE en el servidor. El desplegable de la web
            // solo enseña lo válido, pero la petición se puede construir a mano: sin esto
            // entraría un pedido con un color retirado y se descubriría al ir a imprimirlo.
            Map<String, String> opciones = req.opcionesOVacio();
            validadorOpciones.validar(product, opciones);

            // Snapshot desnormalizado del producto en el momento de la compra.
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImageUrl(product.getImageUrl());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(req.quantity());
            item.setOpciones(serializar(opciones));
            return item;
        }).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        // Lista MUTABLE a propósito: se asigna a la colección @OneToMany (orphanRemoval=true)
        // y Hibernate la modifica al gestionarla. Una lista inmutable (Stream.toList())
        // provocaría UnsupportedOperationException al guardar el pedido.
    }

    /**
     * Las opciones a JSON para guardarlas en la fila del pedido.
     *
     * Si no hay ninguna se guarda null y no una cadena "{}": así se distingue entre «esta
     * tienda no usa opciones» y «se eligieron y estaban vacías», y las filas de las tiendas
     * que no las usan quedan igual que antes.
     */
    private String serializar(Map<String, String> opciones) {
        if (opciones == null || opciones.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(opciones);
        } catch (JsonProcessingException e) {
            // Un mapa de textos siempre se puede serializar; si esto ocurriera sería un
            // fallo de programación, no algo que deba explicarse al cliente.
            throw new IllegalStateException("No se han podido serializar las opciones", e);
        }
    }

    /**
     * Las opciones guardadas, de vuelta a un mapa.
     *
     * Si el texto guardado no se puede leer se devuelve un mapa vacío en lugar de reventar:
     * un pedido antiguo con un formato distinto tiene que poder seguir consultándose. Sería
     * absurdo no poder abrir un pedido por culpa de un campo accesorio.
     */
    private Map<String, String> deserializar(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (JsonProcessingException e) {
            log.warn("Opciones ilegibles en una línea de pedido: {}", json, e);
            return Map.of();
        }
    }

    private Order findById(String id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Pedido no encontrado: " + id));
    }

    private OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream().map(i ->
                new OrderItemDto(i.getProductId(), i.getProductName(), i.getProductImageUrl(),
                                 i.getUnitPrice(), i.getQuantity(), i.subtotal(),
                                 deserializar(i.getOpciones()))
        ).toList();

        return new OrderDto(o.getId(), o.getUserId(), o.getUserEmail(), items,
                o.getShippingCost(), o.getTotal(),
                o.getStatus(), o.getShippingName(), o.getShippingAddress(),
                o.getShippingCity(), o.getShippingPostalCode(), o.getShippingCountry(),
                o.getShippingPhone(), o.getCreatedAt());
    }
}
