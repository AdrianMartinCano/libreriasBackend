package dev.pimon.menu.dto;

import dev.pimon.menu.entity.CategoryIcon;
import dev.pimon.menu.settings.MenuPresentation;

import java.util.List;

/**
 * Carta pública completa lista para pintar: categorías con sus platos disponibles, agrupados y
 * ordenados, más la presentación (fuente/estilos). El front no necesita más llamadas para renderizarla.
 */
public record CartaDto(
        List<CategoryGroup> categories,
        MenuPresentation    presentation
) {
    public record CategoryGroup(
            String        id,
            String        name,
            String        slug,
            String        description,
            CategoryIcon  icon,
            List<MenuItemDto> items
    ) {}
}
