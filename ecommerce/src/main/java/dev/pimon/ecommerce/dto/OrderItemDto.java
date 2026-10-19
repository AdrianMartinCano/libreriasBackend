package dev.pimon.ecommerce.dto;

import java.math.BigDecimal;
import java.util.Map;

public record OrderItemDto(
        String     productId,
        String     productName,
        String     productImageUrl,
        BigDecimal unitPrice,
        int        quantity,
        BigDecimal subtotal,
        /**
         * Lo que el cliente eligió para esta línea: color, dedicatoria, lo que use la
         * tienda. Vacío en las que no usan opciones.
         *
         * SE DEVUELVE SIEMPRE, y hace falta: sin esto, quien prepara el pedido ve "Pulpo
         * articulado ×2" y no sabe de qué color imprimirlos.
         */
        Map<String, String> opciones
) {}
