package dev.pimon.menu.dto;

import dev.pimon.menu.entity.CategoryIcon;
import dev.pimon.menu.entity.MenuCategory;

public record MenuCategoryDto(
        String       id,
        String       name,
        String       slug,
        String       description,
        CategoryIcon icon,
        int          sortOrder,
        boolean      active
) {
    public static MenuCategoryDto from(MenuCategory c) {
        return new MenuCategoryDto(
                c.getId(), c.getName(), c.getSlug(), c.getDescription(),
                c.getIcon(), c.getSortOrder(), c.isActive());
    }
}
