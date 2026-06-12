package dev.pimon.products.mapper;

import dev.pimon.products.dto.CategoryDto;
import dev.pimon.products.dto.ProductDto;
import dev.pimon.products.entity.Category;
import dev.pimon.products.entity.Product;

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
                p.getCategory() != null ? toCategoryDto(p.getCategory()) : null,
                p.getCreatedAt()
        );
    }
}
