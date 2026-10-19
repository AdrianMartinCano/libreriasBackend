package dev.pimon.forum.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.forum.dto.*;
import dev.pimon.forum.dto.PostDto.ReactionCount;
import dev.pimon.forum.service.ForumService;
import dev.pimon.forum.service.NotificationService;
import dev.pimon.forum.service.ReactionService;
import dev.pimon.forum.service.ReportService;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Foro público. Añadir /api/forum/** a public-paths: leer es público; escribir
 * (abrir hilo, responder, reaccionar, denunciar) exige usuario autenticado —
 * el filtro JWT puebla @CurrentUser si hay token.
 */
@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumService        forum;
    private final ReactionService     reactions;
    private final ReportService       reports;
    private final NotificationService notifications;

    public ForumController(ForumService forum, ReactionService reactions,
                           ReportService reports, NotificationService notifications) {
        this.forum         = forum;
        this.reactions     = reactions;
        this.reports       = reports;
        this.notifications = notifications;
    }

    // ── Categorías ──────────────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(forum.listCategories()));
    }

    @GetMapping("/categories/{slug}/threads")
    public ResponseEntity<ApiResponse<PageResponse<ThreadSummaryDto>>> threads(
            @PathVariable String slug, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(forum.listThreads(slug, pageable)));
    }

    @PostMapping("/categories/{slug}/threads")
    public ResponseEntity<ApiResponse<ThreadDto>> openThread(
            @PathVariable String slug, @Valid @RequestBody CreateThreadRequest req,
            @CurrentUser AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(forum.openThread(slug, req, user), "Hilo creado"));
    }

    // ── Hilos y mensajes ────────────────────────────────────────────────────

    @GetMapping("/threads/{slug}")
    public ResponseEntity<ApiResponse<ThreadDto>> thread(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(forum.getThread(slug)));
    }

    @GetMapping("/threads/{slug}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostDto>>> posts(
            @PathVariable String slug, @PageableDefault(size = 20) Pageable pageable) {
        // Lectura pública: el usuario es opcional (si hay token, personaliza "reacted").
        return ResponseEntity.ok(ApiResponse.ok(forum.listPosts(slug, pageable, currentUserOrNull())));
    }

    @PostMapping("/threads/{slug}/posts")
    public ResponseEntity<ApiResponse<PostDto>> reply(
            @PathVariable String slug, @Valid @RequestBody CreatePostRequest req,
            @CurrentUser AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(forum.reply(slug, req, user), "Mensaje publicado"));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<PostDto>> editPost(
            @PathVariable String id, @Valid @RequestBody UpdatePostRequest req,
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(forum.updateOwnPost(id, req, user), "Mensaje actualizado"));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable String id, @CurrentUser AuthUser user) {
        forum.deleteOwnPost(id, user);
        return ResponseEntity.ok(ApiResponse.noContent("Mensaje eliminado"));
    }

    // ── Reacciones ──────────────────────────────────────────────────────────

    @PostMapping("/posts/{id}/reactions")
    public ResponseEntity<ApiResponse<List<ReactionCount>>> react(
            @PathVariable String id, @Valid @RequestBody ReactRequest req,
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(reactions.toggle(id, req.type(), user)));
    }

    // ── Denuncias ───────────────────────────────────────────────────────────

    @PostMapping("/posts/{id}/report")
    public ResponseEntity<ApiResponse<Void>> report(
            @PathVariable String id, @Valid @RequestBody CreateReportRequest req,
            @CurrentUser AuthUser user) {
        reports.create(id, req, user);
        return ResponseEntity.ok(ApiResponse.noContent("Gracias, lo revisaremos"));
    }

    // ── Búsqueda ────────────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ThreadSummaryDto>>> search(
            @RequestParam String q, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(forum.search(q, pageable)));
    }

    // ── Notificaciones (del usuario) ────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> notifications(
            @PageableDefault(size = 20) Pageable pageable, @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(notifications.list(requireId(user), pageable)));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unread(@CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", notifications.unreadCount(requireId(user)))));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable String id, @CurrentUser AuthUser user) {
        notifications.markRead(requireId(user), id);
        return ResponseEntity.ok(ApiResponse.noContent("Marcada como leída"));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@CurrentUser AuthUser user) {
        notifications.markAllRead(requireId(user));
        return ResponseEntity.ok(ApiResponse.noContent("Todas marcadas como leídas"));
    }

    /** Usuario autenticado si hay token (en rutas públicas), o null si es anónimo. */
    private static AuthUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        return null;
    }

    private static String requireId(AuthUser user) {
        if (user == null || user.id() == null) {
            throw dev.pimon.common.exception.AppException.unauthorized("Debes iniciar sesión");
        }
        return user.id();
    }
}
