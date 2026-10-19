package dev.pimon.blog.web;

import dev.pimon.blog.dto.PostDto;
import dev.pimon.blog.dto.PostSummaryDto;
import dev.pimon.blog.dto.SavePostRequest;
import dev.pimon.blog.service.BlogService;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import dev.pimon.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Gestión del blog — solo ADMIN. */
@RestController
@RequestMapping("/api/admin/blog")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlogController {

    private final BlogService service;

    public AdminBlogController(BlogService service) {
        this.service = service;
    }

    /** Todos los posts, incluidos borradores. */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryDto>>> list(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.listAll(pageable)));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<PostDto>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostDto>> create(
            @Valid @RequestBody SavePostRequest req, @CurrentUser AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(req, user), "Entrada creada"));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<PostDto>> update(
            @PathVariable String id, @Valid @RequestBody SavePostRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Entrada actualizada"));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Entrada eliminada"));
    }

    /** Borrar un comentario (moderación). */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable String commentId) {
        service.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.noContent("Comentario eliminado"));
    }
}
