package dev.pimon.ecommerce.event;

import dev.pimon.ecommerce.dto.OrderDto;
import dev.pimon.ecommerce.entity.OrderStatus;

/**
 * Un pedido ha cambiado de estado.
 *
 * <h2>Para qué existe</h2>
 *
 * {@code spring-ecommerce} sabe cuándo entra un pago, pero <b>no sabe qué hacer con esa
 * noticia</b>: eso depende de la tienda. Una manda un correo, otra avisa por Telegram, otra
 * imprime una etiqueta. Sin esta costura, la única forma de reaccionar era meter el correo
 * dentro de la librería, y entonces el texto sería el mismo para todas — «tu pedido está en
 * camino» sirve igual de mal para una figura impresa bajo demanda que para un libro
 * firmado.
 *
 * <h2>Por qué un evento y no otro bean como CalculadorEnvio</h2>
 *
 * {@link dev.pimon.ecommerce.service.CalculadorEnvio} y
 * {@link dev.pimon.ecommerce.service.ValidadorOpciones} son cosas que la librería
 * <b>pregunta</b>: necesita la respuesta para seguir, y por eso son interfaces con un
 * valor por defecto. Esto es algo que <b>anuncia</b>: no espera nada de vuelta, y pueden
 * escucharlo cero o cinco sitios a la vez sin cambiar nada aquí.
 *
 * <h2>Escúchalo DESPUÉS DE CONFIRMAR, no durante</h2>
 *
 * Se publica dentro de la transacción que guarda el pedido. Un {@code @EventListener} normal
 * corre ahí dentro, así que si la transacción se deshace después habrías mandado un correo
 * sobre un pedido que no existe. Para cualquier cosa que salga al exterior:
 *
 * <pre>{@code
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * void avisar(PedidoCambioEstado e) {
 *     if (e.pasoA(OrderStatus.PAID)) { ... }
 * }
 * }</pre>
 *
 * <h2>Se publican TODOS los cambios</h2>
 *
 * También los automáticos: el pago que falla, el pedido que caduca. Filtrar aquí sería
 * decidir por la tienda qué merece un aviso, y hay quien querrá avisar de que un pedido
 * caducó. Quien escucha elige.
 *
 * @param pedido   cómo queda el pedido, ya con el estado nuevo
 * @param anterior de dónde venía, para poder distinguir «acaba de pagarse» de un reenvío
 */
public record PedidoCambioEstado(OrderDto pedido, OrderStatus anterior) {

    /** El estado al que ha llegado. */
    public OrderStatus nuevo() {
        return pedido.status();
    }

    /** Atajo para el uso habitual: {@code if (e.pasoA(OrderStatus.PAID))}. */
    public boolean pasoA(OrderStatus estado) {
        return nuevo() == estado;
    }
}
