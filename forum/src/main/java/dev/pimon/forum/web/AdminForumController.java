package dev.pimon.forum.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.forum.dto.CategoryDto;
import dev.pimon.forum.dto.ReportDto;
import dev.pimon.forum.dto.SaveCategoryRequest;
import dev.pimon.forum.service.ForumService;
import dev.pimon.forum.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión y moderación del foro — solo ADMIN. */
@RestController
@RequestMapping("/api/admin/forum")
@PreAuthorize("hasRole('ADMIN')")
public class AdminForumController {

    private final ForumService  forum;
    private final ReportService reports;

    public AdminForumController(ForumService forum, ReportService reports) {
        this.forum   = forum;
        this.reports = reports;
    }

    // ── Categorías ──────────────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(forum.adminListCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(@Valid @RequestBody SaveCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(forum.createCategory(req), "Categoría creada"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @PathVariable String id, @Valid @RequestBody SaveCategoryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(forum.updateCategory(id, req), "Categoría actualizada"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable String id, @RequestParam(defaultValue = "false") boolean force) {
        forum.deleteCategory(id, force);
        return ResponseEntity.ok(ApiResponse.noContent("Categoría eliminada"));
    }

    // ── Moderación de hilos ─────────────────────────────────────────────────

    @PostMapping("/threads/{id}/pin")
    public ResponseEntity<ApiResponse<Void>> pin(
            @PathVariable String id, @RequestParam(defaultValue = "true") boolean value) {
        forum.setPinned(id, value);
        return ResponseEntity.ok(ApiResponse.noContent(value ? "Hilo fijado" : "Hilo no fijado"));
    }

    @PostMapping("/threads/{id}/lock")
    public ResponseEntity<ApiResponse<Void>> lock(
            @PathVariable String id, @RequestParam(defaultValue = "true") boolean value) {
        forum.setLocked(id, value);
        return ResponseEntity.ok(ApiResponse.noContent(value ? "Hilo cerrado" : "Hilo reabierto"));
    }

    @PostMapping("/threads/{id}/move")
    public ResponseEntity<ApiResponse<Void>> moveThread(
            @PathVariable String id, @RequestParam String categoryId) {
        forum.moveThread(id, categoryId);
        return ResponseEntity.ok(ApiResponse.noContent("Hilo movido"));
    }

    @DeleteMapping("/threads/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThread(@PathVariable String id) {
        forum.deleteThread(id);
        return ResponseEntity.ok(ApiResponse.noContent("Hilo eliminado"));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable String id) {
        forum.deletePost(id);
        return ResponseEntity.ok(ApiResponse.noContent("Mensaje eliminado"));
    }

    // ── Cola de denuncias ───────────────────────────────────────────────────

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PageResponse<ReportDto>>> reports(
            @RequestParam(defaultValue = "true") boolean onlyOpen,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reports.list(onlyOpen, pageable)));
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable String id) {
        reports.resolve(id);
        return ResponseEntity.ok(ApiResponse.noContent("Denuncia resuelta"));
    }
}
