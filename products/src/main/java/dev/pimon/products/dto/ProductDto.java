package dev.pimon.products.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDto(
        String      id,
        String      name,
        String      slug,
        String      description,
        BigDecimal  price,
        BigDecimal  originalPrice,
        Integer     discount,        // % calculado automáticamente
        int         stock,
        boolean     inStock,
        String      imageUrl,
        String      badge,

        /**
         * Las categorías del producto, ordenadas por nombre.
         *
         * <b>Era una sola hasta la 0.10.0</b> ({@code CategoryDto category}). Ahora es una
         * lista porque un producto puede estar en varias a la vez, y nunca es {@code null}:
         * un producto sin ninguna devuelve la lista vacía. Quien la pinte no tiene que
         * comprobar el nulo antes de recorrerla.
         */
        List<CategoryDto> categories,

        /**
         * Si esta publicado.
         *
         * <b>Faltaba hasta la 0.7.0</b>, y su ausencia era peor que la de un dato
         * cualquiera: el panel no podia saber el estado de un producto, asi que pintaba
         * «oculto» para todos y su boton de publicar/ocultar mandaba siempre lo mismo. Un
         * interruptor que no puede leer lo que conmuta no conmuta nada.
         *
         * El listado publico solo devuelve activos, asi que ahi vale {@code true} siempre;
         * el del panel devuelve los dos, y es donde este campo hace falta.
         */
        boolean     active,

        LocalDateTime createdAt
) {}
