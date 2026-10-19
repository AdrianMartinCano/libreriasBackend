package dev.pimon.ecommerce.service;

import dev.pimon.ecommerce.config.EcommerceProperties;
import dev.pimon.ecommerce.dto.CheckoutRequest;
import dev.pimon.ecommerce.entity.OrderItem;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Envío de precio fijo, gratis a partir de cierto importe.
 *
 * Es el comportamiento que la librería ha tenido siempre, movido tal cual desde el
 * método privado del checkout. Se registra automáticamente si el proyecto no declara su
 * propio {@link CalculadorEnvio}, así que las tiendas que ya funcionaban con
 * {@code pimon.ecommerce.shipping.cost} siguen igual sin tocar nada.
 *
 * Cuándo deja de servir: cuando lo que se manda tiene tamaños distintos. Un transportista
 * cobra por lo que ocupa la caja, y con una tarifa fija los pedidos grandes se pagan
 * solos con el margen de los pequeños — hasta que dejan de hacerlo.
 */
@RequiredArgsConstructor
public class EnvioTarifaPlana implements CalculadorEnvio {

    private final EcommerceProperties props;

    @Override
    public BigDecimal calcular(List<OrderItem> items, CheckoutRequest request, BigDecimal subtotal) {
        BigDecimal cost = props.getShipping().getCost();
        if (cost == null || cost.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal freeFrom = props.getShipping().getFreeFrom();
        if (freeFrom != null && subtotal.compareTo(freeFrom) >= 0) {
            return BigDecimal.ZERO;
        }
        return cost.setScale(2, RoundingMode.HALF_UP);
    }
}
