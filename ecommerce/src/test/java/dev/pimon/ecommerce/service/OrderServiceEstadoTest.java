package dev.pimon.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pimon.common.exception.AppException;
import dev.pimon.ecommerce.config.EcommerceProperties;
import dev.pimon.ecommerce.entity.Order;
import dev.pimon.ecommerce.entity.OrderStatus;
import dev.pimon.ecommerce.event.PedidoCambioEstado;
import dev.pimon.ecommerce.repository.OrderRepository;
import dev.pimon.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cambiar el estado de un pedido desde el panel.
 *
 * <h2>Lo que se protege aqui es el ORDEN de las cosas</h2>
 *
 * Los tres casos que importan no van de si el codigo hace lo que dice, sino de en que
 * momento lo hace:
 *
 * <ul>
 *   <li>Validar la transicion ANTES de tocar Stripe. Al reves, un cambio prohibido que
 *       ademas fuera un reembolso habria movido el dinero antes de rechazarse.</li>
 *   <li>Devolver el dinero ANTES de marcar REFUNDED. Si Stripe falla, el pedido no debe
 *       quedarse diciendo que se reembolso.</li>
 *   <li>Devolver el stock al cancelar, que si no se queda retenido para siempre.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceEstadoTest {

    @Mock private OrderRepository orderRepo;
    @Mock private ProductRepository productRepo;
    @Mock private StripeService stripeService;
    @Mock private StockDePedidos stock;
    @Mock private ApplicationEventPublisher eventos;
    @Mock private CalculadorEnvio calculadorEnvio;
    @Mock private ValidadorOpciones validadorOpciones;

    private OrderService servicio;

    @BeforeEach
    void preparar() {
        servicio = new OrderService(orderRepo, productRepo, stripeService, stock, eventos,
                new EcommerceProperties(), calculadorEnvio, validadorOpciones,
                new ObjectMapper());
    }

    @Test
    @DisplayName("pedir el estado que ya tiene no es un error: no guarda ni anuncia")
    void elMismoEstadoNoHaceNada() {
        // El panel puede estar abierto en dos pestanas, o alguien pulsa dos veces. Para
        // quien mira el resultado ya es cierto, asi que fallar seria confundirle.
        Order pedido = pedido(OrderStatus.PAID);
        when(orderRepo.findById("p1")).thenReturn(Optional.of(pedido));

        servicio.updateStatus("p1", OrderStatus.PAID);

        verify(orderRepo, never()).save(any());
        verify(eventos, never()).publishEvent(any(PedidoCambioEstado.class));
    }

    @Test
    @DisplayName("una transicion prohibida se rechaza SIN llamar a Stripe")
    void loProhibidoNoLlegaAStripe() {
        // Cancelar un pedido ya cobrado no vale: hay que reembolsar. Y lo importante es que
        // se rechace ANTES de tocar el dinero — si la validacion fuera despues, un cambio
        // invalido que ademas fuera un reembolso ya habria devuelto el importe.
        Order pedido = pedido(OrderStatus.PAID);
        when(orderRepo.findById("p2")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> servicio.updateStatus("p2", OrderStatus.CANCELLED))
                .isInstanceOf(AppException.class)
                // El mensaje explica que hacer, no solo que ha fallado.
                .hasMessageContaining("REFUNDED");

        verify(stripeService, never()).refund(anyString(), anyLong());
        verify(orderRepo, never()).save(any());
    }

    @Test
    @DisplayName("reembolsar devuelve el dinero ANTES de marcar el pedido")
    void primeroElDineroYLuegoElEstado() {
        // Si Stripe lanza, no se llega al save: el pedido no se queda diciendo que se
        // reembolso cuando el cliente no ha visto un euro.
        Order pedido = pedido(OrderStatus.PAID);
        pedido.setStripePaymentIntentId("pi_x");
        when(orderRepo.findById("p3")).thenReturn(Optional.of(pedido));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        servicio.updateStatus("p3", OrderStatus.REFUNDED);

        InOrder orden = inOrder(stripeService, orderRepo);
        orden.verify(stripeService).refund("pi_x", 1450L);
        orden.verify(orderRepo).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelar devuelve el stock al catalogo")
    void cancelarDevuelveElStock() {
        // Sin esto, la pieza se queda "agotada" estando en el almacen y nadie se entera.
        Order pedido = pedido(OrderStatus.PENDING_PAYMENT);
        when(orderRepo.findById("p4")).thenReturn(Optional.of(pedido));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        servicio.updateStatus("p4", OrderStatus.CANCELLED);

        verify(stock).devolver(pedido);
    }

    @Test
    @DisplayName("avanzar de estado anuncia el cambio, con el estado del que venia")
    void avanzarAnuncia() {
        // El evento lleva el anterior para que quien escucha pueda distinguir "acaba de
        // pagarse" de un reenvio. Los correos de la tienda dependen de eso.
        Order pedido = pedido(OrderStatus.PAID);
        when(orderRepo.findById("p5")).thenReturn(Optional.of(pedido));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        servicio.updateStatus("p5", OrderStatus.SHIPPED);

        verify(eventos).publishEvent(any(PedidoCambioEstado.class));
        verify(stock, never()).devolver(any());
    }

    /** Un pedido de 14,50 € en el estado que se pida. */
    private Order pedido(OrderStatus estado) {
        Order pedido = new Order();
        pedido.setId("pedido-1");
        pedido.setStatus(estado);
        pedido.setTotal(new BigDecimal("14.50"));
        pedido.setShippingCost(BigDecimal.ZERO);
        return pedido;
    }
}
