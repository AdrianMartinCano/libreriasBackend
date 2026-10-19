package dev.pimon.ecommerce.service;

import dev.pimon.ecommerce.config.EcommerceProperties;
import dev.pimon.ecommerce.entity.Order;
import dev.pimon.ecommerce.entity.OrderStatus;
import dev.pimon.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La regla que gobierna la caducidad: <b>ante la duda, NO se cancela</b>.
 *
 * <h2>Por que este es el fichero de tests mas importante del modulo</h2>
 *
 * Los dos errores posibles no cuestan lo mismo, y de eso va todo lo que se prueba aqui:
 *
 * <ul>
 *   <li>Dejar colgado un pedido muerto retiene stock. Se ve en el panel y se arregla a
 *       mano en un minuto.</li>
 *   <li>Cancelar un pedido que SI se pago deja el dinero cobrado, el pedido cancelado y la
 *       pieza vendida otra vez a otra persona. El cliente se entera antes que tu.</li>
 * </ul>
 *
 * Un proceso automatico que cancela pedidos cada cinco minutos sin nadie mirando es
 * exactamente el sitio donde un «pequeno arreglo» hace mucho dano en silencio. Estos tests
 * estan para que ese arreglo falle aqui y no en produccion.
 */
@ExtendWith(MockitoExtension.class)
class CaducidadPedidosTest {

    @Mock private OrderRepository orderRepo;
    @Mock private OrderService pedidos;
    @Mock private StripeService stripe;

    private EcommerceProperties props;
    private CaducidadPedidos caducidad;

    @BeforeEach
    void preparar() {
        props = new EcommerceProperties();
        props.getCaducidad().setEspera(Duration.ofMinutes(30));
        props.getStripe().setEnabled(true);

        caducidad = new CaducidadPedidos(orderRepo, pedidos, stripe, props);
    }

    // ── Lo que NO se debe cancelar ────────────────────────────────────────

    @Test
    @DisplayName("un pedido cuyo pago SI entro no se cancela, aunque siga pendiente")
    void noSeCancelaSiElPagoEntro() {
        // Este es el caso del webhook perdido: el dinero esta cobrado y el pedido se quedo
        // en PENDING_PAYMENT porque el aviso de Stripe no llego. Cancelarlo seria el peor
        // resultado posible de toda la tienda.
        Order pedido = pedidoPendiente("pi_123");
        when(orderRepo.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(pedido));
        when(stripe.estadoDelPago("pi_123")).thenReturn(Optional.of("succeeded"));

        int cancelados = caducidad.caducarPendientes();

        verify(pedidos, never()).cancelarPorCaducidad(any());
        assertThat(cancelados).isZero();
    }

    @Test
    @DisplayName("un pago en curso tampoco se toca")
    void noSeCancelaUnPagoEnCurso() {
        // `processing` y `requires_capture` cuentan igual que `succeeded`: el cobro esta en
        // marcha o retenido, y cancelar seria meterse en medio de una operacion de dinero.
        Order pedido = pedidoPendiente("pi_456");
        when(orderRepo.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(pedido));
        when(stripe.estadoDelPago("pi_456")).thenReturn(Optional.of("processing"));

        caducidad.caducarPendientes();

        verify(pedidos, never()).cancelarPorCaducidad(any());
    }

    @Test
    @DisplayName("si Stripe no contesta, NO se cancela: no saber no es saber que no")
    void anteLaDudaNoSeCancela() {
        // Red caida, clave mal, Stripe de mantenimiento. Con el cobro activo, un `empty`
        // significa «no he podido preguntar», y de ahi no se deduce que no haya dinero.
        Order pedido = pedidoPendiente("pi_789");
        when(orderRepo.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(pedido));
        when(stripe.estadoDelPago("pi_789")).thenReturn(Optional.empty());

        int cancelados = caducidad.caducarPendientes();

        verify(pedidos, never()).cancelarPorCaducidad(any());
        assertThat(cancelados).isZero();
    }

    // ── Lo que SI se cancela ──────────────────────────────────────────────

    @Test
    @DisplayName("un pago que nunca se completo se cancela y devuelve su stock")
    void seCancelaLoQueNoSePago() {
        Order pedido = pedidoPendiente("pi_abc");
        when(orderRepo.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(pedido));
        when(stripe.estadoDelPago("pi_abc")).thenReturn(Optional.of("requires_payment_method"));

        int cancelados = caducidad.caducarPendientes();

        // Quien cancela es OrderService, que es tambien quien devuelve el stock y anuncia
        // el cambio. Aqui solo se decide QUE se cancela.
        verify(pedidos, times(1)).cancelarPorCaducidad(pedido);
        assertThat(cancelados).isEqualTo(1);
    }

    @Test
    @DisplayName("con Stripe apagado se caduca sin preguntar: no hay dinero real")
    void enModoSimuladoSeCaduca() {
        // El `empty` significa aqui otra cosa distinta: no es que no se haya podido
        // preguntar, es que no hay a quien. Un pago simulado no existe en ninguna parte.
        props.getStripe().setEnabled(false);
        Order pedido = pedidoPendiente("pi_demo_x");
        when(orderRepo.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(pedido));
        when(stripe.estadoDelPago(anyString())).thenReturn(Optional.empty());

        int cancelados = caducidad.caducarPendientes();

        verify(pedidos, times(1)).cancelarPorCaducidad(pedido);
        assertThat(cancelados).isEqualTo(1);
    }

    // ── Lo que no debe pasar nunca ────────────────────────────────────────

    @Test
    @DisplayName("sin pedidos vencidos no se llama a Stripe")
    void sinVencidosNoSePreguntaNada() {
        // Corre cada cinco minutos: si preguntase a Stripe con la lista vacia, seria una
        // llamada externa cada cinco minutos para nada, todos los dias.
        when(orderRepo.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());

        int cancelados = caducidad.caducarPendientes();

        verifyNoInteractions(stripe);
        verifyNoInteractions(pedidos);
        assertThat(cancelados).isZero();
    }

    @Test
    @DisplayName("solo mira los pendientes, y solo los mas viejos que la espera")
    void buscaLosCorrectos() {
        when(orderRepo.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());

        LocalDateTime antes = LocalDateTime.now().minusMinutes(30);
        caducidad.caducarPendientes();
        LocalDateTime despues = LocalDateTime.now().minusMinutes(30);

        // El corte tiene que caer dentro de la ventana de esta prueba: si alguien cambiara
        // el signo de la resta, el corte se iria al futuro y caducaria TODO.
        verify(orderRepo).findByStatusAndCreatedAtBefore(
                eq(OrderStatus.PENDING_PAYMENT),
                argThat(corte -> !corte.isBefore(antes) && !corte.isAfter(despues)));
    }

    /** Un pedido pendiente y ya vencido. La fecha da igual: el repositorio esta simulado. */
    private Order pedidoPendiente(String paymentIntentId) {
        Order pedido = new Order();
        pedido.setId("pedido-" + paymentIntentId);
        pedido.setStatus(OrderStatus.PENDING_PAYMENT);
        pedido.setStripePaymentIntentId(paymentIntentId);
        return pedido;
    }
}
