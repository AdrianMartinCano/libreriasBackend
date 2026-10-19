package dev.pimon.blog.web;

import dev.pimon.blog.dto.CommentDto;
import dev.pimon.blog.dto.CreateCommentRequest;
import dev.pimon.blog.dto.PostDto;
import dev.pimon.blog.dto.PostSummaryDto;
import dev.pimon.blog.service.BlogService;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Blog público. Añadir /api/blog/** a public-paths (leer es público; comentar
 * exige usuario autenticado — el filtro JWT puebla @CurrentUser si hay token).
 */
@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService service;

    public BlogController(BlogService service) {
        this.service = service;
    }

    /** Feed: posts publicados, más recientes primero. */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryDto>>> list(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.listPublished(pageable)));
    }

    /** Detalle de un post por slug. */
    @GetMapping("/posts/{slug}")
    public ResponseEntity<ApiResponse<PostDto>> get(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(service.getPublishedBySlug(slug)));
    }

    /** Comentarios de un post (lectura pública). */
    @GetMapping("/posts/{slug}/comments")
    public ResponseEntity<ApiResponse<List<CommentDto>>> comments(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(service.comments(slug)));
    }

    /** Comentar (requiere estar registrado). */
    @PostMapping("/posts/{slug}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> comment(
            @PathVariable String slug,
            @Valid @RequestBody CreateCommentRequest req,
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(service.addComment(slug, req, user), "Comentario publicado"));
    }
}
