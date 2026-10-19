package dev.pimon.menu.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.menu.dto.CartaDto;
import dev.pimon.menu.dto.DailyMenuDto;
import dev.pimon.menu.dto.MenuCategoryDto;
import dev.pimon.menu.service.CartaService;
import dev.pimon.menu.service.DailyMenuService;
import dev.pimon.menu.service.MenuCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API pública del menú (sin autenticación). La carta y el menú del día son agregados independientes:
 * la carta siempre está; el menú del día depende de la fecha.
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class PublicMenuController {

    private final CartaService        cartaService;
    private final DailyMenuService    dailyMenuService;
    private final MenuCategoryService categoryService;

    /** Carta completa: categorías con platos disponibles + presentación (fuente/estilos). */
    @GetMapping
    public ApiResponse<CartaDto> carta() {
        return ApiResponse.ok(cartaService.publicCarta());
    }

    /** Solo las categorías activas (p. ej. para un índice de la carta). */
    @GetMapping("/categories")
    public ApiResponse<List<MenuCategoryDto>> categories() {
        return ApiResponse.ok(categoryService.listActive());
    }

    /** Menús del día publicados de una fecha (por defecto, hoy). */
    @GetMapping("/daily")
    public ApiResponse<List<DailyMenuDto>> daily(@RequestParam(required = false) String date) {
        LocalDate day = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.ok(dailyMenuService.listPublished(day));
    }
}
