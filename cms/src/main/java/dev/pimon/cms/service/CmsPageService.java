package dev.pimon.cms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import dev.pimon.cms.dto.CmsPageDto;
import dev.pimon.cms.dto.CmsPageSummaryDto;
import dev.pimon.cms.dto.SaveCmsPageRequest;
import dev.pimon.cms.entity.CmsPage;
import dev.pimon.cms.repository.CmsPageRepository;
import dev.pimon.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CmsPageService {

    private final CmsPageRepository repo;
    private final ObjectMapper       mapper;

    // ── Público ──────────────────────────────────────────────────────────

    /** Devuelve una página publicada por su slug */
    @Transactional(readOnly = true)
    public CmsPageDto getPublishedBySlug(String slug) {
        return toDto(repo.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> AppException.notFound("Página no encontrada: " + slug)));
    }

    /** Lista todas las páginas publicadas (sin bloques) */
    @Transactional(readOnly = true)
    public List<CmsPageSummaryDto> listPublished() {
        return repo.findByPublishedTrueOrderByUpdatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    /** Lista todas las páginas (publicadas y borradores) */
    @Transactional(readOnly = true)
    public List<CmsPageSummaryDto> listAll() {
        return repo.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CmsPageDto getById(String id) {
        return toDto(findById(id));
    }

    /** Crea una nueva página. El slug debe ser único. */
    public CmsPageDto create(SaveCmsPageRequest req) {
        if (repo.existsBySlug(req.slug())) {
            throw AppException.conflict("Ya existe una página con el slug: " + req.slug());
        }
        CmsPage page = new CmsPage();
        apply(page, req);
        return toDto(repo.save(page));
    }

    /** Actualiza una página existente */
    public CmsPageDto update(String id, SaveCmsPageRequest req) {
        CmsPage page = findById(id);

        // Si el slug cambia, verificar que no esté en uso
        if (!page.getSlug().equals(req.slug()) && repo.existsBySlug(req.slug())) {
            throw AppException.conflict("Ya existe una página con el slug: " + req.slug());
        }

        apply(page, req);
        return toDto(repo.save(page));
    }

    /** Publica o despublica una página */
    public CmsPageDto togglePublished(String id) {
        CmsPage page = findById(id);
        page.setPublished(!page.isPublished());
        return toDto(repo.save(page));
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw AppException.notFound("Página no encontrada: " + id);
        }
        repo.deleteById(id);
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private void apply(CmsPage page, SaveCmsPageRequest req) {
        page.setSlug(req.slug());
        page.setTitle(req.title());
        page.setDescription(req.description());
        page.setPublished(req.published());
        page.setBlocksJson(serialize(req.blocks() != null ? req.blocks() : List.of()));
    }

    private CmsPage findById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Página no encontrada: " + id));
    }

    private CmsPageDto toDto(CmsPage p) {
        return new CmsPageDto(p.getId(), p.getSlug(), p.getTitle(), p.getDescription(),
                              deserialize(p.getBlocksJson()), p.getUpdatedAt());
    }

    private CmsPageSummaryDto toSummary(CmsPage p) {
        return new CmsPageSummaryDto(p.getId(), p.getSlug(), p.getTitle(),
                                     p.getDescription(), p.isPublished(), p.getUpdatedAt());
    }

    private String serialize(List<Map<String, Object>> blocks) {
        try {
            return mapper.writeValueAsString(blocks);
        } catch (JsonProcessingException e) {
            throw AppException.badRequest("Los bloques no son JSON válido: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deserialize(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
