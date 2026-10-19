package dev.pimon.products.mapper;

import dev.pimon.products.dto.CategoryDto;
import dev.pimon.products.entity.Category;
import dev.pimon.products.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que el DTO no pierda por el camino lo que el panel necesita.
 *
 * <h2>De donde sale este test</h2>
 *
 * De un fallo real y de los caros de encontrar: {@code ProductDto} no incluia
 * {@code active}. En el navegador ese campo llegaba {@code undefined}, con dos efectos que
 * no parecian relacionados entre si:
 *
 * <ul>
 *   <li>La etiqueta de estado pintaba «oculto» para TODOS los productos, porque
 *       {@code undefined} es falso.</li>
 *   <li>El boton de publicar/ocultar calculaba lo contrario de {@code undefined}, que
 *       siempre es {@code true}, asi que cada clic mandaba «publicalo» y nunca ocultaba
 *       nada.</li>
 * </ul>
 *
 * <b>Un interruptor que no puede leer lo que conmuta no conmuta nada.</b> Y no daba ningun
 * error: el PUT respondia 200 tan tranquilo.
 */
class ProductMapperTest {

    @Test
    @DisplayName("un producto publicado llega al DTO como publicado")
    void llevaElActivo() {
        assertThat(ProductMapper.toDto(producto(true)).active()).isTrue();
    }

    @Test
    @DisplayName("un producto oculto llega al DTO como oculto")
    void llevaElOculto() {
        // Este es el que fallaba: sin el campo, el panel no podia distinguirlos.
        assertThat(ProductMapper.toDto(producto(false)).active()).isFalse();
    }

    @Test
    @DisplayName("un producto sin categorias devuelve lista vacia, nunca null")
    void sinCategoriasDevuelveVacio() {
        // Un null aqui obligaria a comprobarlo en cada plantilla que las recorra, y basta
        // con que se olvide una para que la ficha reviente en el navegador de un cliente.
        assertThat(ProductMapper.toDto(producto(true)).categories()).isEmpty();
    }

    @Test
    @DisplayName("las categorias llegan TODAS al DTO, no solo la primera")
    void llevaTodasLasCategorias() {
        // Es el test de la 0.10.0: con una sola categoria esto no podia fallar. Ahora una
        // pieza puede ser «Coleccionismo» y «Halloween» a la vez, y si el mapper se dejara
        // una por el camino el filtro del catalogo la perderia sin dar ningun error.
        Product p = producto(true);
        p.setCategories(new LinkedHashSet<>(List.of(
                categoria("c1", "Coleccionismo"),
                categoria("c2", "Halloween"))));

        assertThat(ProductMapper.toDto(p).categories())
                .extracting(CategoryDto::name)
                .containsExactly("Coleccionismo", "Halloween");
    }

    private Category categoria(String id, String nombre) {
        Category c = new Category();
        c.setId(id);
        c.setName(nombre);
        c.setSlug(nombre.toLowerCase());
        return c;
    }

    private Product producto(boolean activo) {
        Product p = new Product();
        p.setId("p1");
        p.setName("Pulpo articulado");
        p.setSlug("pulpo-articulado");
        p.setPrice(new BigDecimal("10.00"));
        p.setStock(3);
        p.setActive(activo);
        return p;
    }
}
