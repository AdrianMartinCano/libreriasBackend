package dev.pimon.products.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.products.dto.*;
import dev.pimon.products.entity.Category;
import dev.pimon.products.entity.Product;
import dev.pimon.products.mapper.ProductMapper;
import dev.pimon.products.repository.CategoryRepository;
import dev.pimon.products.repository.ProductRepository;
import dev.pimon.products.repository.ProductSpecification;
import dev.pimon.products.util.SlugUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository  productRepo;
    private final CategoryRepository categoryRepo;

    /**
     * Para el panel: TODOS los productos, publicados y ocultos.
     *
     * Quien administra tiene que ver lo que ha retirado, o retirarlo seria un viaje sin
     * vuelta. Es el mismo criterio que con los colores y los complementos.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductDto> listAll(ProductFilter filter, Pageable pageable) {
        return PageResponse.from(
                productRepo.findAll(ProductSpecification.withFilter(filter, false), pageable)
                        .map(ProductMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> list(ProductFilter filter, Pageable pageable) {
        return PageResponse.from(
                productRepo.findAll(ProductSpecification.withFilter(filter), pageable)
                           .map(ProductMapper::toDto)
        );
    }

    @Transactional(readOnly = true)
    public ProductDto getBySlug(String slug) {
        return ProductMapper.toDto(
                productRepo.findBySlugAndActiveTrue(slug)
                        .orElseThrow(() -> AppException.notFound("Producto no encontrado: " + slug))
        );
    }

    @Transactional(readOnly = true)
    public ProductDto getById(String id) {
        return ProductMapper.toDto(findById(id));
    }

    public ProductDto create(CreateProductRequest req) {
        Set<Category> categorias = findCategories(req.categoryIds());
        String slug = uniqueSlug(SlugUtils.toSlug(req.name()));

        Product p = new Product();
        p.setName(req.name());
        p.setSlug(slug);
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setOriginalPrice(req.originalPrice());
        p.setStock(req.stock());
        p.setImageUrl(req.imageUrl());
        p.setBadge(req.badge());
        p.setCategories(categorias);

        // null = publicado, que es como se comportaba esto antes de existir el campo: quien
        // ya llamaba a este endpoint no tiene que cambiar nada.
        if (req.active() != null) {
            p.setActive(req.active());
        }

        return ProductMapper.toDto(productRepo.save(p));
    }

    public ProductDto update(String id, UpdateProductRequest req) {
        Product p = findById(id);

        if (req.name()          != null) p.setName(req.name());
        if (req.description()   != null) p.setDescription(req.description());
        if (req.price()         != null) p.setPrice(req.price());
        if (req.originalPrice() != null) p.setOriginalPrice(req.originalPrice());
        if (req.stock()         != null) p.setStock(req.stock());
        if (req.imageUrl()      != null) p.setImageUrl(req.imageUrl());
        if (req.badge()         != null) p.setBadge(req.badge());
        if (req.active()        != null) p.setActive(req.active());
        // Sustituye la lista entera. Un null aquí es «no las toques», como el resto de
        // campos; una lista vacía la rechaza findCategories.
        if (req.categoryIds()   != null) p.setCategories(findCategories(req.categoryIds()));

        return ProductMapper.toDto(productRepo.save(p));
    }

    public void delete(String id) {
        Product p = findById(id);
        p.setActive(false);
        productRepo.save(p);
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private Product findById(String id) {
        return productRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Producto no encontrado: " + id));
    }

    /**
     * Resuelve los ids a categorías, o falla diciendo cuál no existe.
     *
     * <h2>Una consulta, no una por id</h2>
     *
     * {@code findAllById} las trae todas de golpe. Son pocas —dos o tres por producto—
     * pero pedirlas de una en una es el mismo N+1 que se evita en el listado, y aquí se
     * ejecuta en cada alta y en cada edición.
     *
     * <h2>Si falta una, se dice CUÁL</h2>
     *
     * Comparar solo los tamaños daría «faltan categorías» sin decir cuál, y quien lo lea
     * tendrá delante tres ids que no distingue de un vistazo.
     *
     * <h2>Vacío se rechaza aquí y no con anotaciones</h2>
     *
     * Porque el {@code PUT} del panel no lleva {@code @Valid} —es parcial a propósito— así
     * que una lista vacía llegaría hasta la entidad sin que nadie la mirase, y dejaría un
     * producto sin ninguna categoría: algo que el alta no permite crear.
     */
    private Set<Category> findCategories(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw AppException.badRequest("El producto tiene que estar en al menos una categoría");
        }

        Map<String, Category> encontradas = categoryRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        Set<Category> resultado = new LinkedHashSet<>();
        for (String id : ids) {
            Category c = encontradas.get(id);
            if (c == null) {
                throw AppException.notFound("Categoría no encontrada: " + id);
            }
            resultado.add(c);
        }
        return resultado;
    }

    /** Garantiza que el slug sea único añadiendo sufijo numérico si es necesario */
    private String uniqueSlug(String base) {
        String slug = base;
        int i = 1;
        while (productRepo.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
