package dev.pimon.products.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.products.dto.CreateCategoryRequest;
import dev.pimon.products.entity.Category;
import dev.pimon.products.repository.CategoryRepository;
import dev.pimon.products.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Borrar y crear categorias.
 *
 * <h2>De donde salen estos tests</h2>
 *
 * De un fallo real: {@code delete} solo ponia {@code active = false}, y como el panel unica-
 * mente pinta las activas, la categoria parecia borrada pero seguia ocupando su nombre. Al
 * intentar crearla otra vez saltaba «Ya existe una categoria con ese nombre», refiriendose
 * a algo que no se veia por ningun lado. Sin salida desde la interfaz.
 *
 * Lo que se prueba aqui es justo lo que evita repetirlo: que borrar borra, que no borra lo
 * que tiene contenido, y que un nombre retirado se puede recuperar.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository repo;
    @Mock private ProductRepository productRepo;
    @InjectMocks private CategoryService service;

    // ── Borrar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("una categoria vacia se borra de verdad, no se oculta")
    void seBorraDeVerdad() {
        Category vacia = new Category("Figuras", "figuras", null, null);
        when(repo.findById("c1")).thenReturn(Optional.of(vacia));
        when(productRepo.countByCategoriesId("c1")).thenReturn(0L);

        service.delete("c1");

        verify(repo).delete(vacia);
    }

    @Test
    @DisplayName("con productos dentro no se borra: quedarian colgados de una fila que no existe")
    void conProductosNoSeBorra() {
        Category conCosas = new Category("Figuras", "figuras", null, null);
        when(repo.findById("c2")).thenReturn(Optional.of(conCosas));
        when(productRepo.countByCategoriesId("c2")).thenReturn(3L);
        when(productRepo.countByCategoriesIdAndActiveTrue("c2")).thenReturn(3L);

        assertThatThrownBy(() -> service.delete("c2"))
                .isInstanceOf(AppException.class)
                // El mensaje tiene que decir CUANTOS y QUE hacer: "no se puede borrar" a
                // secas deja a quien lo lee sin saber por donde seguir.
                .hasMessageContaining("Figuras")
                .hasMessageContaining("3")
                // Desde la 0.10.0 lo que hay que hacer es QUITARLES ESTA, no cambiarlos de
                // categoria: con varias, los productos se quedan en las demas.
                .hasMessageContaining("Quítasela");

        verify(repo, never()).delete(any());
    }

    @Test
    @DisplayName("los productos de la papelera tambien cuentan, y se dicen aparte")
    void losDeLaPapeleraCuentan() {
        // Se pueden restaurar: si su categoria hubiera desaparecido, restaurar devolveria
        // un producto roto. Y se mencionan aparte porque en la lista de productos NO se
        // ven, asi que sin decirlo el aviso pareceria equivocado.
        Category conPapelera = new Category("Prototipos", "prototipos", null, null);
        when(repo.findById("c3")).thenReturn(Optional.of(conPapelera));
        when(productRepo.countByCategoriesId("c3")).thenReturn(4L);
        when(productRepo.countByCategoriesIdAndActiveTrue("c3")).thenReturn(1L);

        assertThatThrownBy(() -> service.delete("c3"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("1 a la venta")
                .hasMessageContaining("3 en la papelera");
    }

    // ── Crear ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear con el nombre de una retirada la recupera en vez de fallar")
    void recuperaUnaRetirada() {
        // Esto es lo que arregla las que ya se habian quedado desactivadas: se vuelven a
        // dar de alta y reaparecen, con los datos nuevos.
        Category retirada = new Category("Figuras", "figuras", "vieja", null);
        retirada.setActive(false);
        when(repo.findBySlug("figuras")).thenReturn(Optional.of(retirada));
        when(repo.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        service.create(new CreateCategoryRequest("Figuras", "nueva", null));

        ArgumentCaptor<Category> guardada = ArgumentCaptor.forClass(Category.class);
        verify(repo).save(guardada.capture());
        assertThat(guardada.getValue().isActive()).isTrue();
        assertThat(guardada.getValue().getDescription()).isEqualTo("nueva");
    }

    @Test
    @DisplayName("crear con el nombre de una ACTIVA sigue chocando")
    void conUnaActivaChoca() {
        // Aqui el error si se entiende: la categoria se ve en la lista.
        Category activa = new Category("Figuras", "figuras", null, null);
        when(repo.findBySlug("figuras")).thenReturn(Optional.of(activa));

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Figuras", null, null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Ya existe");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("un nombre libre crea una categoria nueva")
    void nombreLibreCrea() {
        when(repo.findBySlug("moldes")).thenReturn(Optional.empty());
        when(repo.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        service.create(new CreateCategoryRequest("Moldes", null, null));

        verify(repo).save(any(Category.class));
    }
}
