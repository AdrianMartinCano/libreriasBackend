package dev.pimon.menu.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.menu.dto.*;
import dev.pimon.menu.service.DailyMenuService;
import dev.pimon.menu.service.MenuCategoryService;
import dev.pimon.menu.service.MenuItemService;
import dev.pimon.menu.service.MenuPresentationService;
import dev.pimon.menu.settings.MenuPresentation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Panel de administración de la carta, el menú del día y la presentación. */
@RestController
@RequestMapping("/api/admin/menu")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuCategoryService     categoryService;
    private final MenuItemService         itemService;
    private final DailyMenuService        dailyMenuService;
    private final MenuPresentationService presentationService;

    /* ── Presentación (fuente/estilos) ── */

    @GetMapping("/presentation")
    public ApiResponse<MenuPresentation> getPresentation() {
        return ApiResponse.ok(presentationService.current());
    }

    @PutMapping("/presentation")
    public ApiResponse<MenuPresentation> updatePresentation(@Valid @RequestBody MenuPresentation presentation) {
        return ApiResponse.ok(presentationService.update(presentation), "Presentación guardada");
    }

    /* ── Categorías ── */

    @GetMapping("/categories")
    public ApiResponse<List<MenuCategoryDto>> listCategories() {
        return ApiResponse.ok(categoryService.listAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<MenuCategoryDto>> createCategory(
            @Valid @RequestBody CreateMenuCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.create(req), "Categoría creada"));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<MenuCategoryDto> updateCategory(
            @PathVariable String id, @Valid @RequestBody CreateMenuCategoryRequest req) {
        return ApiResponse.ok(categoryService.update(id, req));
    }

    @PatchMapping("/categories/{id}/toggle-active")
    public ApiResponse<Void> toggleCategory(@PathVariable String id) {
        categoryService.toggleActive(id);
        return ApiResponse.ok(null, "Categoría actualizada");
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable String id) {
        categoryService.delete(id);
        return ApiResponse.ok(null, "Categoría eliminada");
    }

    /* ── Platos ── */

    @GetMapping("/items")
    public ApiResponse<List<MenuItemDto>> listItems(@RequestParam String categoryId) {
        return ApiResponse.ok(itemService.listByCategory(categoryId));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<MenuItemDto>> createItem(@Valid @RequestBody CreateMenuItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(itemService.create(req), "Plato creado"));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<MenuItemDto> updateItem(@PathVariable String id, @Valid @RequestBody CreateMenuItemRequest req) {
        return ApiResponse.ok(itemService.update(id, req));
    }

    @PatchMapping("/items/{id}/toggle-available")
    public ApiResponse<Void> toggleItem(@PathVariable String id) {
        itemService.toggleAvailable(id);
        return ApiResponse.ok(null, "Disponibilidad actualizada");
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable String id) {
        itemService.delete(id);
        return ApiResponse.ok(null, "Plato eliminado");
    }

    /* ── Menú del día ── */

    /** Menús (cualquier estado) de una fecha. */
    @GetMapping("/daily")
    public ApiResponse<List<DailyMenuDto>> listDaily(@RequestParam String date) {
        return ApiResponse.ok(dailyMenuService.listForDate(LocalDate.parse(date)));
    }

    /** Histórico por rango de fechas. */
    @GetMapping("/daily/range")
    public ApiResponse<List<DailyMenuDto>> listDailyRange(
            @RequestParam String from, @RequestParam String to) {
        return ApiResponse.ok(dailyMenuService.listRange(LocalDate.parse(from), LocalDate.parse(to)));
    }

    /** Histórico paginado de menús del día (más recientes primero) — para consultar menús antiguos. */
    @GetMapping("/daily/history")
    public ApiResponse<PageResponse<DailyMenuDto>> listDailyHistory(@ParameterObject Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(dailyMenuService.listPaged(pageable)));
    }

    @GetMapping("/daily/{id}")
    public ApiResponse<DailyMenuDto> getDaily(@PathVariable String id) {
        return ApiResponse.ok(dailyMenuService.findById(id));
    }

    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<DailyMenuDto>> createDaily(@Valid @RequestBody SaveDailyMenuRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyMenuService.create(req), "Menú del día creado"));
    }

    @PutMapping("/daily/{id}")
    public ApiResponse<DailyMenuDto> updateDaily(@PathVariable String id, @Valid @RequestBody SaveDailyMenuRequest req) {
        return ApiResponse.ok(dailyMenuService.update(id, req));
    }

    @PostMapping("/daily/{id}/publish")
    public ApiResponse<DailyMenuDto> publishDaily(@PathVariable String id) {
        return ApiResponse.ok(dailyMenuService.publish(id), "Menú publicado");
    }

    @PostMapping("/daily/{id}/unpublish")
    public ApiResponse<DailyMenuDto> unpublishDaily(@PathVariable String id) {
        return ApiResponse.ok(dailyMenuService.unpublish(id), "Menú despublicado");
    }

    /** Clona el menú a otra fecha (borrador). Ej.: POST /daily/{id}/clone?toDate=2026-06-21 */
    @PostMapping("/daily/{id}/clone")
    public ResponseEntity<ApiResponse<DailyMenuDto>> cloneDaily(
            @PathVariable String id, @RequestParam String toDate) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyMenuService.clone(id, toDate), "Menú clonado"));
    }

    @DeleteMapping("/daily/{id}")
    public ApiResponse<Void> deleteDaily(@PathVariable String id) {
        dailyMenuService.delete(id);
        return ApiResponse.ok(null, "Menú eliminado");
    }
}
