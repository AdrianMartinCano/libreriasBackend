package dev.pimon.products.dto;

import java.math.BigDecimal;
import java.util.List;

/** Todos los campos son opcionales — solo se actualizan los no nulos. */
public record UpdateProductRequest(
        String     name,
        String     description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer    stock,
        String  imageUrl,
        String  badge,

        /**
         * Las categorías, <b>sustituidas enteras</b> cuando viene.
         *
         * {@code null} es «no las toques», como el resto de campos de este DTO. Una lista
         * <b>vacía se rechaza</b> en lugar de dejar el producto sin ninguna: al crearlo se
         * exige al menos una, así que permitir quitárselas todas al editar dejaría un
         * producto imposible de crear pero posible de conseguir editando.
         */
        List<String> categoryIds,

        Boolean active
) {}
