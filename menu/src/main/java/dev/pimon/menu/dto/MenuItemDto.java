package dev.pimon.menu.dto;

import dev.pimon.menu.entity.Allergen;
import dev.pimon.menu.entity.MenuItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record MenuItemDto(
        String                id,
        String                name,
        String                description,
        BigDecimal            price,
        List<PricedPortionDto> portions,
        Set<Allergen>         allergens,
        boolean               vegan,
        boolean               vegetarian,
        boolean               glutenFree,
        boolean               spicy,
        String                badge,
        boolean               available,
        int                   sortOrder,
        String                categoryId,
        String                categoryName
) {
    public static MenuItemDto from(MenuItem i) {
        return new MenuItemDto(
                i.getId(), i.getName(), i.getDescription(), i.getPrice(),
                i.getPortions().stream().map(PricedPortionDto::from).toList(),
                i.getAllergens(),
                i.isVegan(), i.isVegetarian(), i.isGlutenFree(), i.isSpicy(),
                i.getBadge(), i.isAvailable(), i.getSortOrder(),
                i.getCategory().getId(), i.getCategory().getName());
    }
}
