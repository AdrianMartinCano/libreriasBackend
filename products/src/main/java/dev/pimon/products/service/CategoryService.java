package dev.pimon.products.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.exception.AppException;
import dev.pimon.products.dto.CategoryDto;
import dev.pimon.products.dto.CreateCategoryRequest;
import dev.pimon.products.entity.Category;
import dev.pimon.products.mapper.ProductMapper;
import dev.pimon.products.repository.CategoryRepository;
import dev.pimon.products.repository.ProductRepository;
import dev.pimon.products.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository repo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public List<CategoryDto> listActive() {
        return repo.findByActiveTrueOrderByName().stream()
                .map(ProductMapper::toCategoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getBySlug(String slug) {
        return ProductMapper.toCategoryDto(
                repo.findBySlugAndActiveTrue(slug)
                        .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + slug))
        );
    }

    /**
     * Crea la categoría, o revive una que estuviera retirada con ese mismo nombre.
     *
     * <h2>Por qué revive en vez de rechazar</h2>
     *
     * Antes, una categoría retirada seguía ocupando su slug y el alta fallaba con «ya
     * existe una categoría con ese nombre». Desde el panel eso no había manera de
     * resolverlo: la retirada <b>no se ve en ninguna parte</b>, así que el mensaje señalaba
     * algo invisible y el nombre quedaba quemado para siempre.
     *
     * Ahora volver a darla de alta la recupera, con los datos nuevos. Solo choca de verdad
     * cuando la que existe está activa, y entonces sí se ve en la lista y el mensaje se
     * entiende.
     */
    public CategoryDto create(CreateCategoryRequest req) {
        String slug = SlugUtils.toSlug(req.name());

        Category existente = repo.findBySlug(slug).orElse(null);
        if (existente != null) {
            if (existente.isActive()) {
                throw AppException.conflict("Ya existe una categoría con ese nombre");
            }
            existente.setName(req.name());
            existente.setDescription(req.description());
            existente.setImageUrl(req.imageUrl());
            existente.setActive(true);
            log.info("Categoría '{}' recuperada: estaba retirada", req.name());
            return ProductMapper.toCategoryDto(repo.save(existente));
        }

        return ProductMapper.toCategoryDto(
                repo.save(new Category(req.name(), slug, req.description(), req.imageUrl()))
        );
    }

    public CategoryDto update(String id, CreateCategoryRequest req) {
        Category cat = repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + id));
        cat.setName(req.name());
        if (req.description() != null) cat.setDescription(req.description());
        if (req.imageUrl()    != null) cat.setImageUrl(req.imageUrl());
        return ProductMapper.toCategoryDto(repo.save(cat));
    }

    /**
     * Borra la categoría de verdad, si no hay nada dentro.
     *
     * <h2>Antes solo la desactivaba, y eso engañaba</h2>
     *
     * Desaparecía de la lista —el panel solo pinta las activas— así que parecía borrada,
     * pero seguía en la base ocupando su nombre. Quien intentaba volver a crearla se
     * encontraba un «ya existe» refiriéndose a algo que no veía por ningún lado.
     *
     * <h2>Y si tiene productos, no se borra: se dice por qué</h2>
     *
     * Los productos apuntan a la categoría con clave ajena. Borrarla con productos dentro
     * los dejaría colgados, así que se rechaza con la cuenta y con qué hacer.
     *
     * <b>Cuentan también los de la papelera</b>, porque se pueden restaurar: dejar
     * desaparecer su categoría convertiría una restauración en un producto roto. Por eso
     * el mensaje los menciona aparte — si no, se leería como un error, ya que en la lista
     * de productos no se ven.
     */
    public void delete(String id) {
        Category cat = repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + id));

        long total = productRepo.countByCategoriesId(id);
        if (total > 0) {
            long activos = productRepo.countByCategoriesIdAndActiveTrue(id);
            long enPapelera = total - activos;

            String detalle = enPapelera == 0
                    ? activos + (activos == 1 ? " producto" : " productos")
                    : activos + " a la venta y " + enPapelera + " en la papelera";

            // «Cámbialos de categoría» era la salida cuando cada producto tenía una sola.
            // Con varias, lo que hay que hacer es quitarles ESTA, que no es lo mismo:
            // seguirán en las demás y no hay que buscarles un sitio nuevo.
            throw AppException.badRequest(
                    "No se puede borrar «" + cat.getName() + "»: tiene " + detalle
                    + ". Quítasela a esos productos primero.");
        }

        repo.delete(cat);
        log.info("Categoría '{}' borrada", cat.getName());
    }
}
