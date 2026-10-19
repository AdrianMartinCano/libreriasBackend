package dev.pimon.menu.service;

import dev.pimon.menu.dto.CartaDto;
import dev.pimon.menu.dto.MenuItemDto;
import dev.pimon.menu.entity.MenuCategory;
import dev.pimon.menu.entity.MenuItem;
import dev.pimon.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compone la carta pública: platos disponibles agrupados por categoría + presentación. */
@Service
@RequiredArgsConstructor
public class CartaService {

    private final MenuItemRepository      itemRepo;
    private final MenuPresentationService presentationService;

    @Transactional(readOnly = true)
    public CartaDto publicCarta() {
        // findPublicCarta() ya viene ordenado por categoría y por plato; se agrupa preservando el orden.
        Map<String, CartaDto.CategoryGroup> byCategory = new LinkedHashMap<>();
        Map<String, List<MenuItemDto>>      items      = new LinkedHashMap<>();

        for (MenuItem item : itemRepo.findPublicCarta()) {
            MenuCategory c = item.getCategory();
            items.computeIfAbsent(c.getId(), k -> new ArrayList<>()).add(MenuItemDto.from(item));
            byCategory.computeIfAbsent(c.getId(), k -> new CartaDto.CategoryGroup(
                    c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getIcon(), null));
        }

        List<CartaDto.CategoryGroup> groups = byCategory.values().stream()
                .map(g -> new CartaDto.CategoryGroup(
                        g.id(), g.name(), g.slug(), g.description(), g.icon(), items.get(g.id())))
                .toList();

        return new CartaDto(groups, presentationService.current());
    }
}
