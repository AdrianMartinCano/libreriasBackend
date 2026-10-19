package dev.pimon.forum.repository;

import dev.pimon.forum.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumPostRepository extends JpaRepository<ForumPost, String> {

    Page<ForumPost> findByThreadIdOrderByCreatedAtAsc(String threadId, Pageable pageable);

    List<ForumPost> findByThreadIdOrderByCreatedAtAsc(String threadId);

    Optional<ForumPost> findByThreadIdAndOpeningTrue(String threadId);

    Optional<ForumPost> findFirstByThreadIdOrderByCreatedAtDesc(String threadId);

    long countByThreadIdAndOpeningFalse(String threadId);

    void deleteByThreadId(String threadId);

    /** Búsqueda por cuerpo del mensaje (devuelve ids de hilo para agrupar). */
    Page<ForumPost> findByBodyContainingIgnoreCaseOrderByCreatedAtDesc(String q, Pageable pageable);
}
