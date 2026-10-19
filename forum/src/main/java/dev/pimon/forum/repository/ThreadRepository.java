package dev.pimon.forum.repository;

import dev.pimon.forum.entity.ForumThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThreadRepository extends JpaRepository<ForumThread, String> {

    /** Hilos de una categoría: fijados primero, luego por actividad reciente. */
    Page<ForumThread> findByCategoryIdOrderByPinnedDescLastPostAtDesc(String categoryId, Pageable pageable);

    Optional<ForumThread> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByCategoryId(String categoryId);

    /** Búsqueda por título. */
    Page<ForumThread> findByTitleContainingIgnoreCaseOrderByLastPostAtDesc(String q, Pageable pageable);
}
