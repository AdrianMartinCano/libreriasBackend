package dev.pimon.blog.repository;

import dev.pimon.blog.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {

    /** Feed público: solo publicados, más recientes primero. */
    Page<Post> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    /** Detalle público por slug (solo si está publicado). */
    Optional<Post> findBySlugAndPublishedTrue(String slug);

    /** Listado admin (incluye borradores). */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsBySlug(String slug);
}
