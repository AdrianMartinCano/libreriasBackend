package dev.pimon.products.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.products.dto.CreateProductRequest;
import dev.pimon.products.entity.Category;
import dev.pimon.products.entity.Product;
import dev.pimon.products.repository.CategoryRepository;
import dev.pimon.products.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Crear un producto: publicado o no, y en cuántas categorías.
 *
 * <h2>De dónde sale esto</h2>
 *
 * De un fallo real en una tienda que consume la librería: al crear una pieza salía al
 * catálogo <b>en el mismo momento de guardarla</b>, sin fotos y sin medidas — y ahí sin
 * medidas ni siquiera se le puede calcular el envío, así que estaba a la venta sin poder
 * venderse. Su panel mandaba {@code active: false} para evitarlo y no servía de nada,
 * porque este DTO no tenía el campo: se ignoraba en silencio.
 *
 * <h2>Los tres casos de `active`, y por qué los tres</h2>
 *
 * El de {@code null} es el que protege a quien ya usaba esto: si alguien añade el campo y
 * cambia el valor por defecto, todas las tiendas que ya existen empiezan a crear productos
 * ocultos sin haber tocado una línea. Ese es el test que de verdad hay que tener.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceCrearTest {

    @Mock private ProductRepository  productRepo;
    @Mock private CategoryRepository categoryRepo;
    @InjectMocks private ProductService service;

    // ── Publicado o no ────────────────────────────────────────────────────

    @Test
    @DisplayName("sin decir nada, el producto se crea publicado")
    void porDefectoPublicado() {
        assertThat(crearCon(null).isActive()).isTrue();
    }

    @Test
    @DisplayName("con active = false se crea sin publicar, para prepararlo antes de venderlo")
    void sinPublicar() {
        assertThat(crearCon(false).isActive()).isFalse();
    }

    @Test
    @DisplayName("con active = true se crea publicado")
    void publicado() {
        assertThat(crearCon(true).isActive()).isTrue();
    }

    // ── Categorías (0.10.0) ───────────────────────────────────────────────

    @Test
    @DisplayName("se guardan TODAS las categorias que se pidan, no solo la primera")
    void guardaVariasCategorias() {
        when(categoryRepo.findAllById(any()))
                .thenReturn(List.of(categoria("cat-1", "Coleccionismo"),
                                    categoria("cat-2", "Halloween")));
        when(productRepo.existsBySlug(any())).thenReturn(false);
        when(productRepo.save(any(Product.class))).thenAnswer(l -> l.getArgument(0));

        service.create(peticion(List.of("cat-1", "cat-2"), null));

        assertThat(guardado().getCategories())
                .extracting(Category::getName)
                .containsExactly("Coleccionismo", "Halloween");
    }

    @Test
    @DisplayName("si una categoria no existe, se dice CUAL y no se guarda nada")
    void categoriaInexistente() {
        // Decir «faltan categorías» a secas deja delante tres ids que no se distinguen de
        // un vistazo. Y sobre todo: el producto no puede quedarse creado a medias.
        when(categoryRepo.findAllById(any()))
                .thenReturn(List.of(categoria("cat-1", "Coleccionismo")));

        assertThatThrownBy(() -> service.create(peticion(List.of("cat-1", "fantasma"), null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("fantasma");

        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("sin ninguna categoria no se crea: no aparecería en ningún filtro")
    void sinCategoriasNoSeCrea() {
        assertThatThrownBy(() -> service.create(peticion(List.of(), null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("al menos una categoría");

        verify(productRepo, never()).save(any());
    }

    // ── Apoyo ─────────────────────────────────────────────────────────────

    /** Crea un producto con ese valor de `active` y devuelve lo que se guardó. */
    private Product crearCon(Boolean active) {
        when(categoryRepo.findAllById(any()))
                .thenReturn(List.of(categoria("cat-1", "Figuras")));
        when(productRepo.existsBySlug(any())).thenReturn(false);
        when(productRepo.save(any(Product.class))).thenAnswer(l -> l.getArgument(0));

        service.create(peticion(List.of("cat-1"), active));

        return guardado();
    }

    private CreateProductRequest peticion(List<String> categoryIds, Boolean active) {
        return new CreateProductRequest(
                "Pulpo articulado", "Se mueve entero.", new BigDecimal("14.90"),
                null, 5, null, null, categoryIds, active);
    }

    private Product guardado() {
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(captor.capture());
        return captor.getValue();
    }

    private Category categoria(String id, String nombre) {
        Category c = new Category();
        c.setId(id);
        c.setName(nombre);
        return c;
    }
}
