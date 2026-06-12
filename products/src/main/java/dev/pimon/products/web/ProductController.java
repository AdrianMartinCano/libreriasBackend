package dev.pimon.products.web;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.products.dto.ProductDto;
import dev.pimon.products.dto.ProductFilter;
import dev.pimon.products.service.ProductService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints públicos del catálogo.
 * Añadir /api/products/** a public-paths en libui.security.public-paths.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    /** Lista productos con filtros opcionales */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductDto>>> list(
            @ModelAttribute ProductFilter filter,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(filter, pageable)));
    }

    /** Detalle de un producto por slug */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDto>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(service.getBySlug(slug)));
    }
}
