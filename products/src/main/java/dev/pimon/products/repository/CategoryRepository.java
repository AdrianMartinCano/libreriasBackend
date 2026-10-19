package dev.pimon.products.repository;

import dev.pimon.products.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByActiveTrueOrderByName();
    Optional<Category> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);

    /**
     * Por slug, este activa o no.
     *
     * Hace falta para poder revivir una categoria retirada en vez de rechazar el alta con
     * un "ya existe" que el usuario no puede resolver: no la ve por ningun lado.
     */
    Optional<Category> findBySlug(String slug);
}
