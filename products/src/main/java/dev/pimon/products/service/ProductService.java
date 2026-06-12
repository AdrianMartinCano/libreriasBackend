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

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository  productRepo;
    private final CategoryRepository categoryRepo;

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
        Category category = findCategory(req.categoryId());
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
        p.setCategory(category);

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
        if (req.categoryId()    != null) p.setCategory(findCategory(req.categoryId()));

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

    private Category findCategory(String categoryId) {
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + categoryId));
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
