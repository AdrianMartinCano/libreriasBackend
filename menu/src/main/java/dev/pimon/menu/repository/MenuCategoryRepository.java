package dev.pimon.menu.repository;

import dev.pimon.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, String> {

    List<MenuCategory> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<MenuCategory> findAllByOrderBySortOrderAscNameAsc();

    boolean existsBySlug(String slug);

    Optional<MenuCategory> findBySlug(String slug);
}
