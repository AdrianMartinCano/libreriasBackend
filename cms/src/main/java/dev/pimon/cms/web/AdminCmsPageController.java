package dev.pimon.cms.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.cms.dto.CmsPageDto;
import dev.pimon.cms.dto.CmsPageSummaryDto;
import dev.pimon.cms.dto.SaveCmsPageRequest;
import dev.pimon.cms.service.CmsPageService;
import dev.pimon.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cms/pages")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
@RequiredArgsConstructor
public class AdminCmsPageController {

    private final CmsPageService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CmsPageSummaryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CmsPageDto>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CmsPageDto>> create(
            @Valid @RequestBody SaveCmsPageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(req), "Página creada correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CmsPageDto>> update(
            @PathVariable String id,
            @Valid @RequestBody SaveCmsPageRequest req) {
        return ResponseEntity.ok(
                ApiResponse.ok(service.update(id, req), "Página actualizada correctamente"));
    }

    /** Publica o despublica una página */
    @PatchMapping("/{id}/toggle-published")
    public ResponseEntity<ApiResponse<CmsPageDto>> togglePublished(@PathVariable String id) {
        CmsPageDto page = service.togglePublished(id);
        String msg = page.blocks() != null && page.slug() != null
                ? (service.listPublished().stream().anyMatch(p -> p.id().equals(id))
                   ? "Página publicada" : "Página guardada como borrador")
                : "Estado actualizado";
        return ResponseEntity.ok(ApiResponse.ok(page, msg));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Página eliminada correctamente"));
    }
}
