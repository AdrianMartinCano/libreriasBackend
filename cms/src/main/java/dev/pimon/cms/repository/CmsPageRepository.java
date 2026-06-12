package dev.pimon.cms.repository;

import dev.pimon.cms.entity.CmsPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CmsPageRepository extends JpaRepository<CmsPage, String> {
    Optional<CmsPage> findBySlugAndPublishedTrue(String slug);
    Optional<CmsPage> findBySlug(String slug);
    List<CmsPage> findByPublishedTrueOrderByUpdatedAtDesc();
    boolean existsBySlug(String slug);
}
