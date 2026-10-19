package dev.pimon.ecommerce.service;

import dev.pimon.ecommerce.entity.Order;
import dev.pimon.ecommerce.entity.OrderItem;
import dev.pimon.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Devuelve al catalogo las unidades que reservo un pedido que no salio adelante.
 *
 * <h2>Por que esto existe aparte</h2>
 *
 * Hay TRES caminos por los que un pedido se queda sin comprarse, y los tres tienen que
 * devolver el stock igual:
 *
 * <ol>
 *   <li>Stripe avisa de que el pago fallo ({@code payment_intent.payment_failed}).</li>
 *   <li>El pedido caduca esperando un pago que no llega.</li>
 *   <li>Alguien lo cancela desde el panel.</li>
 * </ol>
 *
 * Con la resta repetida en cada sitio, el dia que cambie algo —un descuento, un apartado—
 * se arregla en dos y se olvida el tercero, y eso no da error: solo deja piezas
 * «agotadas» que estan en el almacen.
 *
 * <h2>Quien llama comprueba el estado ANTES</h2>
 *
 * Esto suma y no pregunta nada: llamarlo dos veces para el mismo pedido devolveria el
 * doble. Los tres caminos miran el estado antes de cancelar y solo cancelan desde
 * {@code PENDING_PAYMENT}, asi que un segundo intento se topa con el pedido ya cancelado y
 * no llega hasta aqui.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDePedidos {

    private final ProductRepository productRepo;

    /**
     * Suma otra vez lo que el pedido habia reservado.
     *
     * Linea a linea y no de golpe porque cada una puede fallar por su cuenta: si un
     * producto se borro del catalogo mientras el pedido esperaba, las demas se devuelven
     * igual en vez de perderse todas.
     */
    public void devolver(Order pedido) {
        for (OrderItem linea : pedido.getItems()) {
            int filas = productRepo.incrementStock(linea.getProductId(), linea.getQuantity());
            if (filas == 0) {
                log.warn("No se pudo devolver el stock de {} del pedido {}: "
                         + "el producto ya no existe", linea.getProductId(), pedido.getId());
            }
        }
    }
}
