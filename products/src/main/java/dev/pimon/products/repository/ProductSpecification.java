package dev.pimon.products.repository;

import dev.pimon.products.dto.ProductFilter;
import dev.pimon.products.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> withFilter(ProductFilter f) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (f.search() != null && !f.search().isBlank()) {
                String pattern = "%" + f.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")),        pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (f.category() != null && !f.category().isBlank()) {
                predicates.add(cb.equal(
                        root.join("category").get("slug"), f.category()
                ));
            }

            if (f.minPrice() != null) {
                predicates.add(cb.ge(root.get("price"), f.minPrice()));
            }

            if (f.maxPrice() != null) {
                predicates.add(cb.le(root.get("price"), f.maxPrice()));
            }

            if (Boolean.TRUE.equals(f.inStock())) {
                predicates.add(cb.gt(root.get("stock"), 0));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
