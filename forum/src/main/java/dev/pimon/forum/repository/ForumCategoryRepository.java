package dev.pimon.forum.repository;

import dev.pimon.forum.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumCategoryRepository extends JpaRepository<ForumCategory, String> {
    List<ForumCategory> findAllByOrderByPositionAscNameAsc();
    Optional<ForumCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
