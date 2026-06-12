package dev.pimon.products.repository;

import dev.pimon.products.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByActiveTrueOrderByName();
    Optional<Category> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
}
