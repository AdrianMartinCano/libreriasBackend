package dev.pimon.ecommerce.service;

import dev.pimon.ecommerce.entity.Order;
import dev.pimon.ecommerce.entity.OrderItem;
import dev.pimon.products.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Devolver al catalogo lo que reservo un pedido que no salio adelante.
 *
 * <h2>Por que tiene tests propios siendo tan corto</h2>
 *
 * Porque lo llaman TRES caminos distintos —pago fallido, caducidad y cancelacion manual— y
 * un fallo aqui no da ningun error: solo deja piezas «agotadas» que estan en el almacen.
 * Nadie lo descubre hasta que un cliente pregunta por que no puede comprar algo que se ve
 * en la web.
 */
@ExtendWith(MockitoExtension.class)
class StockDePedidosTest {

    @Mock private ProductRepository productRepo;
    @InjectMocks private StockDePedidos stock;

    @Test
    @DisplayName("devuelve las unidades de cada linea, no solo las de la primera")
    void devuelveTodasLasLineas() {
        Order pedido = new Order();
        pedido.setId("p1");
        pedido.getItems().add(linea("pieza-a", 2));
        pedido.getItems().add(linea("pieza-b", 3));

        when(productRepo.incrementStock("pieza-a", 2)).thenReturn(1);
        when(productRepo.incrementStock("pieza-b", 3)).thenReturn(1);

        stock.devolver(pedido);

        verify(productRepo, times(1)).incrementStock("pieza-a", 2);
        verify(productRepo, times(1)).incrementStock("pieza-b", 3);
        verifyNoMoreInteractions(productRepo);
    }

    @Test
    @DisplayName("si un producto ya no existe, las demas lineas se devuelven igual")
    void unaLineaRotaNoSeLlevaALasDemas() {
        // Pasa de verdad: alguien borra una pieza del catalogo mientras un pedido espera
        // pago. Si esto abortara al primer fallo, el resto del pedido se quedaria sin
        // devolver y nadie se enteraria.
        Order pedido = new Order();
        pedido.setId("p2");
        pedido.getItems().add(linea("borrada", 1));
        pedido.getItems().add(linea("viva", 4));

        when(productRepo.incrementStock("borrada", 1)).thenReturn(0);
        when(productRepo.incrementStock("viva", 4)).thenReturn(1);

        stock.devolver(pedido);

        verify(productRepo).incrementStock("viva", 4);
    }

    @Test
    @DisplayName("un pedido sin lineas no toca la base")
    void sinLineasNoHaceNada() {
        stock.devolver(new Order());

        verifyNoMoreInteractions(productRepo);
    }

    private OrderItem linea(String productId, int cantidad) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(cantidad);
        return item;
    }
}
