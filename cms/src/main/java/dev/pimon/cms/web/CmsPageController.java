package dev.pimon.cms.web;

import lombok.RequiredArgsConstructor;
import dev.pimon.cms.dto.CmsPageDto;
import dev.pimon.cms.dto.CmsPageSummaryDto;
import dev.pimon.cms.service.CmsPageService;
import dev.pimon.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints públicos del CMS.
 * Añadir a public-paths: "/api/cms/**"
 */
@RestController
@RequestMapping("/api/cms")
@RequiredArgsConstructor
public class CmsPageController {

    private final CmsPageService service;

    /** Lista las páginas publicadas (sin bloques — para menú/navegación) */
    @GetMapping("/pages")
    public ResponseEntity<ApiResponse<List<CmsPageSummaryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.listPublished()));
    }

    /**
     * Devuelve una página publicada con todos sus bloques.
     * El frontend llama a este endpoint y pasa la respuesta a <lib-cms-page [page]="data"/>.
     */
    @GetMapping("/pages/{slug}")
    public ResponseEntity<ApiResponse<CmsPageDto>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(service.getPublishedBySlug(slug)));
    }
}
