package dev.pimon.products.mapper;

import dev.pimon.products.dto.CategoryDto;
import dev.pimon.products.dto.ProductDto;
import dev.pimon.products.entity.Category;
import dev.pimon.products.entity.Product;

import java.util.List;
import java.util.Set;

public class ProductMapper {

    private ProductMapper() {}

    public static CategoryDto toCategoryDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(),
                               c.getDescription(), c.getImageUrl());
    }

    public static ProductDto toDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getPrice(),
                p.getOriginalPrice(),
                p.getDiscount(),
                p.getStock(),
                p.isInStock(),
                p.getImageUrl(),
                p.getBadge(),
                toCategoryDtos(p.getCategories()),
                p.isActive(),
                p.getCreatedAt()
        );
    }

    /**
     * Lista vacía y nunca {@code null}, aunque la entidad venga sin colección.
     *
     * Un {@code null} aquí obligaría a comprobarlo en cada plantilla que recorra las
     * categorías, y basta con que se olvide una para que la ficha reviente en el navegador
     * de un cliente.
     */
    private static List<CategoryDto> toCategoryDtos(Set<Category> categorias) {
        if (categorias == null) {
            return List.of();
        }
        return categorias.stream().map(ProductMapper::toCategoryDto).toList();
    }
}
