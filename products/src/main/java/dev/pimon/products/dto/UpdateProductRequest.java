package dev.pimon.products.dto;

/** Todos los campos son opcionales — solo se actualizan los no nulos. */
public record UpdateProductRequest(
        String  name,
        String  description,
        Double  price,
        Double  originalPrice,
        Integer stock,
        String  imageUrl,
        String  badge,
        String  categoryId,
        Boolean active
) {}
