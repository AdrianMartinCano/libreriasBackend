package dev.pimon.menu.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.menu.dto.CreateMenuCategoryRequest;
import dev.pimon.menu.dto.MenuCategoryDto;
import dev.pimon.menu.entity.CategoryIcon;
import dev.pimon.menu.entity.MenuCategory;
import dev.pimon.menu.repository.MenuCategoryRepository;
import dev.pimon.menu.repository.MenuItemRepository;
import dev.pimon.menu.util.MenuSlugs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuCategoryService {

    private final MenuCategoryRepository repo;
    private final MenuItemRepository     itemRepo;

    public List<MenuCategoryDto> listActive() {
        return repo.findByActiveTrueOrderBySortOrderAscNameAsc().stream().map(MenuCategoryDto::from).toList();
    }

    public List<MenuCategoryDto> listAll() {
        return repo.findAllByOrderBySortOrderAscNameAsc().stream().map(MenuCategoryDto::from).toList();
    }

    @Transactional
    public MenuCategoryDto create(CreateMenuCategoryRequest req) {
        String slug = MenuSlugs.toSlug(req.name());
        if (repo.existsBySlug(slug)) {
            throw AppException.conflict("Ya existe una categoría con ese nombre.");
        }
        return MenuCategoryDto.from(repo.save(new MenuCategory(
                req.name(), slug, req.description(), iconOrDefault(req.icon()), req.sortOrder())));
    }

    @Transactional
    public MenuCategoryDto update(String id, CreateMenuCategoryRequest req) {
        MenuCategory c = findOrThrow(id);
        c.setName(req.name());
        c.setDescription(req.description());
        c.setIcon(iconOrDefault(req.icon()));
        c.setSortOrder(req.sortOrder());
        return MenuCategoryDto.from(repo.save(c));
    }

    @Transactional
    public void toggleActive(String id) {
        MenuCategory c = findOrThrow(id);
        c.setActive(!c.isActive());
        repo.save(c);
    }

    @Transactional
    public void delete(String id) {
        if (itemRepo.existsByCategory_Id(id)) {
            throw AppException.conflict("La categoría tiene platos; muévelos o elimínalos antes (o desactívala).");
        }
        repo.delete(findOrThrow(id));
    }

    public MenuCategory findOrThrow(String id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + id));
    }

    private CategoryIcon iconOrDefault(CategoryIcon icon) {
        return icon != null ? icon : CategoryIcon.NONE;
    }
}
