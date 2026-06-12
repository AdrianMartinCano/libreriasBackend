package dev.pimon.products.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.exception.AppException;
import dev.pimon.products.dto.CategoryDto;
import dev.pimon.products.dto.CreateCategoryRequest;
import dev.pimon.products.entity.Category;
import dev.pimon.products.mapper.ProductMapper;
import dev.pimon.products.repository.CategoryRepository;
import dev.pimon.products.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository repo;

    @Transactional(readOnly = true)
    public List<CategoryDto> listActive() {
        return repo.findByActiveTrueOrderByName().stream()
                .map(ProductMapper::toCategoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getBySlug(String slug) {
        return ProductMapper.toCategoryDto(
                repo.findBySlugAndActiveTrue(slug)
                        .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + slug))
        );
    }

    public CategoryDto create(CreateCategoryRequest req) {
        String slug = SlugUtils.toSlug(req.name());
        if (repo.existsBySlug(slug)) {
            throw AppException.conflict("Ya existe una categoría con ese nombre");
        }
        return ProductMapper.toCategoryDto(
                repo.save(new Category(req.name(), slug, req.description(), req.imageUrl()))
        );
    }

    public CategoryDto update(String id, CreateCategoryRequest req) {
        Category cat = repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + id));
        cat.setName(req.name());
        if (req.description() != null) cat.setDescription(req.description());
        if (req.imageUrl()    != null) cat.setImageUrl(req.imageUrl());
        return ProductMapper.toCategoryDto(repo.save(cat));
    }

    public void delete(String id) {
        Category cat = repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Categoría no encontrada: " + id));
        cat.setActive(false);
        repo.save(cat);
    }
}
