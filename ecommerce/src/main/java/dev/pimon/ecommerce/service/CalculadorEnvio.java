package dev.pimon.ecommerce.service;

import dev.pimon.ecommerce.dto.CheckoutRequest;
import dev.pimon.ecommerce.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuánto se cobra de envío por un pedido.
 *
 * <h2>Por qué esto es una interfaz</h2>
 *
 * Antes el envío era un método privado del checkout que aplicaba una tarifa fija. Sirve
 * para una tienda que manda siempre lo mismo —un libro, por ejemplo— pero no para una que
 * manda objetos de tamaños distintos: ahí el transportista cobra por lo que ocupa el
 * paquete, no por lo que vale lo que va dentro.
 *
 * Sin un punto de extensión, la única salida para un proyecto con otro criterio era
 * escribir su propio checkout, y con él una segunda copia de Stripe, los estados y los
 * reembolsos. Duplicar eso es exactamente lo que esta librería existe para evitar.
 *
 * <h2>Cómo se usa</h2>
 *
 * No hay que hacer nada para seguir como antes: {@link EnvioTarifaPlana} se registra sola
 * y lee la configuración de siempre. Para cambiar el criterio, basta con declarar un bean
 * propio de esta interfaz — al estar la implementación por defecto marcada con
 * {@code @ConditionalOnMissingBean}, la del proyecto gana.
 *
 * <pre>
 * &#64;Bean
 * CalculadorEnvio calculadorEnvio(MisTarifas tarifas) {
 *     return (items, req, subtotal) -&gt; ...;
 * }
 * </pre>
 *
 * <h2>Qué se espera de una implementación</h2>
 *
 * <ul>
 *   <li><b>Devolver el coste con IVA</b>, escalado a 2 decimales. Nunca null.</li>
 *   <li><b>Lanzar si el pedido no se puede enviar</b> —zona no servida, peso fuera de
 *       tarifa— en lugar de devolver cero. Un cero aquí es un envío gratis que nadie ha
 *       decidido, y no se descubre hasta ver la factura del transportista.</li>
 *   <li><b>No tocar el pedido.</b> Se llama antes de guardarlo, solo para calcular.</li>
 * </ul>
 */
@FunctionalInterface
public interface CalculadorEnvio {

    /**
     * @param items    las líneas del pedido, ya validadas y con su producto resuelto
     * @param request  los datos del checkout: incluye la dirección y el código postal,
     *                 que es de donde sale la zona de envío
     * @param subtotal la suma de los artículos, sin envío. Sirve para el umbral de
     *                 "envío gratis a partir de X"
     * @return lo que se cobra de envío, con IVA y a 2 decimales
     */
    BigDecimal calcular(List<OrderItem> items, CheckoutRequest request, BigDecimal subtotal);
}
