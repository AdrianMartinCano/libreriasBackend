package dev.pimon.menu.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.menu.dto.CreateMenuItemRequest;
import dev.pimon.menu.dto.MenuItemDto;
import dev.pimon.menu.entity.MenuCategory;
import dev.pimon.menu.entity.MenuItem;
import dev.pimon.menu.entity.PricedPortion;
import dev.pimon.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository     repo;
    private final MenuCategoryService    categoryService;

    public List<MenuItemDto> listByCategory(String categoryId) {
        return repo.findByCategory_IdOrderBySortOrderAscNameAsc(categoryId).stream()
                .map(MenuItemDto::from).toList();
    }

    @Transactional
    public MenuItemDto create(CreateMenuItemRequest req) {
        validate(req);
        MenuItem i = new MenuItem();
        apply(i, req);
        return MenuItemDto.from(repo.save(i));
    }

    @Transactional
    public MenuItemDto update(String id, CreateMenuItemRequest req) {
        validate(req);
        MenuItem i = findOrThrow(id);
        apply(i, req);
        return MenuItemDto.from(repo.save(i));
    }

    @Transactional
    public void toggleAvailable(String id) {
        MenuItem i = findOrThrow(id);
        i.setAvailable(!i.isAvailable());
        repo.save(i);
    }

    @Transactional
    public void delete(String id) {
        repo.delete(findOrThrow(id));
    }

    public MenuItem findOrThrow(String id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Plato no encontrado: " + id));
    }

    /** Un plato debe tener al menos un precio: o {@code price} base, o alguna ración. */
    private void validate(CreateMenuItemRequest req) {
        boolean hasPortions = req.portions() != null && !req.portions().isEmpty();
        if (!hasPortions && req.price() == null) {
            throw AppException.badRequest("Indica un precio o al menos una ración con precio.");
        }
    }

    private void apply(MenuItem i, CreateMenuItemRequest req) {
        MenuCategory category = categoryService.findOrThrow(req.categoryId());
        i.setName(req.name());
        i.setDescription(req.description());
        i.setPrice(req.price());
        i.setPortions(mapPortions(req));
        i.setAllergens(req.allergens() != null ? new LinkedHashSet<>(req.allergens()) : new LinkedHashSet<>());
        i.setVegan(req.vegan());
        i.setVegetarian(req.vegetarian());
        i.setGlutenFree(req.glutenFree());
        i.setSpicy(req.spicy());
        i.setBadge(req.badge());
        i.setSortOrder(req.sortOrder());
        i.setCategory(category);
    }

    private List<PricedPortion> mapPortions(CreateMenuItemRequest req) {
        List<PricedPortion> portions = new ArrayList<>();
        if (req.portions() != null) {
            req.portions().forEach(p -> portions.add(new PricedPortion(p.label(), p.amount())));
        }
        return portions;
    }
}
