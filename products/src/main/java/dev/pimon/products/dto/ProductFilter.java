package dev.pimon.products.dto;

/**
 * Filtros para el listado de productos.
 * Todos son opcionales — si son null no se aplica el filtro.
 *
 * GET /api/products?search=silla&category=mobiliario&minPrice=50&maxPrice=300&inStock=true
 */
public record ProductFilter(
        String  search,       // busca en nombre y descripción
        String  category,     // slug de la categoría
        Double  minPrice,
        Double  maxPrice,
        Boolean inStock       // true = solo con stock > 0
) {}
