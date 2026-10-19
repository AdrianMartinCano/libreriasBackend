package dev.pimon.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "La descripción es obligatoria")
        String description,

        @NotNull @Positive(message = "El precio debe ser mayor que 0")
        BigDecimal price,

        BigDecimal originalPrice,

        @NotNull @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        String imageUrl,
        String badge,

        /**
         * En qué categorías entra. <b>Al menos una.</b>
         *
         * <b>Era un {@code String categoryId} hasta la 0.10.0.</b> Sigue siendo obligatorio
         * poner una: un producto sin categoría no aparece en ningún filtro del catálogo, y
         * eso no se nota al crearlo sino cuando alguien no lo encuentra.
         */
        @NotEmpty(message = "El producto tiene que estar en al menos una categoría")
        List<String> categoryIds,

        /**
         * Si el producto sale al catálogo público nada más crearse.
         *
         * <b>{@code null} significa que sí</b>, que es como se comportaba esto antes de que
         * el campo existiera: quien ya llamaba a este endpoint sigue creando productos
         * publicados sin cambiar nada.
         *
         * Mandar {@code false} sirve para <b>prepararlo antes de venderlo</b>, y no es un
         * capricho: hay tiendas donde un producto recién creado todavía no tiene fotos, ni
         * medidas, ni nada de lo que su web necesita para calcular un envío — porque todo eso
         * cuelga de su id y no puede existir hasta que el producto está guardado. Sin este
         * campo, el rato entre «guardar» y «tenerlo listo» se pasa con el producto a la venta.
         */
        Boolean active
) {}
