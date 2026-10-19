package dev.pimon.ecommerce.dto;

import dev.pimon.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Un pedido, tal y como lo ve el panel y el cliente.
 *
 * <h2>Los datos de envío van COMPLETOS, y hacen falta los cuatro</h2>
 *
 * Hasta la 0.4.0 faltaban el codigo postal y el telefono. La entidad si los guardaba —el
 * checkout los manda— pero no salian por la API, con lo cual desde el panel <b>no se podia
 * rellenar una etiqueta de envio</b>: habia que entrar a la base de datos a buscar el
 * codigo postal de cada pedido.
 *
 * El telefono no es un adorno: es lo que pide el transportista para avisar antes de
 * entregar, y sin el se pierden entregas.
 */
public record OrderDto(
        String          id,
        String          userId,
        String          userEmail,
        List<OrderItemDto> items,
        BigDecimal      shippingCost,
        BigDecimal      total,
        OrderStatus     status,
        String          shippingName,
        String          shippingAddress,
        String          shippingCity,
        String          shippingPostalCode,
        String          shippingCountry,
        String          shippingPhone,
        LocalDateTime   createdAt
) {}
