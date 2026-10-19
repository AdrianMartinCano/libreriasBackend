package dev.pimon.ecommerce.service;

import dev.pimon.ecommerce.config.EcommerceProperties;
import dev.pimon.ecommerce.entity.Order;
import dev.pimon.ecommerce.entity.OrderStatus;
import dev.pimon.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Devuelve al catalogo el stock de los pedidos que nadie llego a pagar.
 *
 * <h2>El problema</h2>
 *
 * {@code createCheckoutIntent} descuenta el stock al crear el pedido. Es lo correcto: sin
 * eso, dos personas podrian comprar la ultima pieza mientras cada una teclea su tarjeta, y
 * una de las dos recibiria un correo de disculpa. Pero tiene una contrapartida: quien llega
 * al pago y cierra la pestana deja ese stock retenido, y sin este proceso <b>no lo devuelve
 * nadie</b>. Una tienda pequena se queda "sin existencias" de algo que tiene en la mano.
 *
 * <h2>La regla que gobierna todo esto</h2>
 *
 * <b>Ante la duda, NO se cancela.</b> Los dos errores posibles no cuestan lo mismo:
 *
 * <ul>
 *   <li>Dejar colgado un pedido muerto retiene stock. Se ve en el panel y se arregla a
 *       mano en un minuto.</li>
 *   <li>Cancelar un pedido que SI se pago es el peor resultado de la tienda entera:
 *       dinero cobrado, pedido cancelado y la pieza vendida otra vez a otra persona. El
 *       cliente se entera antes que tu.</li>
 * </ul>
 *
 * Por eso se pregunta a Stripe antes de tocar nada, y si Stripe no contesta se deja el
 * pedido en paz.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaducidadPedidos {

    /**
     * Estados de Stripe en los que hay dinero de por medio.
     *
     * {@code processing} y {@code requires_capture} cuentan igual que {@code succeeded}: en
     * los tres el cobro esta en marcha o retenido, y cancelar seria meterse en medio.
     */
    private static final Set<String> CON_DINERO =
            Set.of("succeeded", "processing", "requires_capture");

    private final OrderRepository orderRepo;
    private final OrderService pedidos;
    private final StripeService stripeService;
    private final EcommerceProperties props;

    /**
     * Revisa los pendientes vencidos y cancela los que de verdad no se pagaron.
     *
     * @return cuantos se han cancelado
     */
    @Transactional
    public int caducarPendientes() {
        LocalDateTime corte = LocalDateTime.now().minus(props.getCaducidad().getEspera());

        List<Order> vencidos =
                orderRepo.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, corte);

        if (vencidos.isEmpty()) {
            return 0;
        }

        int cancelados = 0;
        for (Order pedido : vencidos) {
            if (hayDineroDePorMedio(pedido)) {
                continue;
            }
            // Aqui se decide QUE se cancela; cancelarlo es cosa de OrderService, que es
            // quien devuelve el stock y anuncia el cambio como cualquier otro.
            pedidos.cancelarPorCaducidad(pedido);
            cancelados++;

            log.info("Pedido {} caducado sin pagar: cancelado y devuelto su stock",
                     pedido.getId());
        }

        if (cancelados > 0) {
            log.info("Caducidad: {} de {} pedidos vencidos cancelados",
                     cancelados, vencidos.size());
        }
        return cancelados;
    }

    /**
     * Si tocar este pedido puede afectar a un cobro.
     *
     * Devuelve {@code true} tambien cuando NO se ha podido averiguar, que es el punto: no
     * saber no es lo mismo que saber que no.
     */
    private boolean hayDineroDePorMedio(Order pedido) {
        Optional<String> estado = stripeService.estadoDelPago(pedido.getStripePaymentIntentId());

        if (estado.isEmpty()) {
            // Vacio significa dos cosas distintas y hay que separarlas: con Stripe apagado
            // el pago era simulado y no hay dinero real, asi que caducar es seguro. Con
            // Stripe encendido significa que no se ha podido preguntar —red, clave mal— y
            // ahi no se toca nada.
            return props.getStripe().isEnabled();
        }

        if (CON_DINERO.contains(estado.get())) {
            // Esto es un webhook perdido, y merece un error en el log y no un aviso: hay
            // un cliente que ha pagado y cuyo pedido nadie esta preparando.
            log.error("Pedido {} sigue PENDING_PAYMENT pero su pago en Stripe esta en '{}'. "
                      + "Se ha perdido un webhook: NO se cancela. Revisalo y pasalo a PAID.",
                      pedido.getId(), estado.get());
            return true;
        }

        return false;
    }

}
