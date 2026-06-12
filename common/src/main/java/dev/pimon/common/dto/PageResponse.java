package dev.pimon.common.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Respuesta paginada estándar.
 *
 * Uso:
 *   Page<Producto> page = productoRepo.findAll(pageable);
 *   return ApiResponse.ok(PageResponse.from(page));
 */
@Getter
@Builder
public class PageResponse<T> {

    private List<T> items;
    private long    total;
    private int     page;
    private int     pageSize;
    private int     totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .items(page.getContent())
                .total(page.getTotalElements())
                .page(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
