package dev.pimon.ecommerce.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de un pedido, y qué camino puede seguir desde cada uno.
 *
 * LAS TRANSICIONES VIVEN AQUÍ, en el enum, y no en el servicio: son conocimiento del
 * dominio, no de la capa que las aplica. Quien mire este fichero ve el ciclo de vida
 * completo de un pedido sin abrir nada más.
 *
 * <pre>
 *   PENDING_PAYMENT ──► PAID ──► PROCESSING ──► SHIPPED ──► DELIVERED
 *         │               │           │            │            │
 *         ▼               └───────────┴────────────┴────────────┘
 *     CANCELLED                            │
 *                                          ▼
 *                                      REFUNDED
 * </pre>
 *
 * Dentro del flujo principal se puede saltar hacia adelante (de PAID directo a SHIPPED,
 * por ejemplo). Lo que no se puede es ir hacia atrás.
 *
 * DOS DECISIONES QUE NO SON OBVIAS:
 *
 * <b>Solo se cancela lo que no se ha cobrado.</b> Desde PAID en adelante la salida es
 * REFUNDED, no CANCELLED. Si se permitiera cancelar un pedido pagado, quedaría cerrado
 * con el dinero del cliente todavía en la cuenta y sin nada que lo delate. REFUNDED
 * devuelve el dinero de verdad, así que es el camino correcto y el único.
 *
 * <b>Se puede reembolsar hasta después de entregado.</b> No es un descuido: en España el
 * comprador tiene 14 días de desistimiento desde que recibe el pedido, así que DELIVERED
 * no puede ser un estado final.
 */
public enum OrderStatus {
    PENDING_PAYMENT,  // creada, esperando pago
    PAID,             // Stripe confirmó el pago
    PROCESSING,       // preparando el envío
    SHIPPED,          // enviado
    DELIVERED,        // entregado
    CANCELLED,        // cancelado (solo si nunca se cobró)
    REFUNDED;         // devuelto, con el dinero de vuelta en Stripe

    /**
     * Si desde este estado se puede pasar al indicado.
     *
     * Pasar al MISMO estado no se considera una transición válida: quien llame debe
     * tratarlo aparte, normalmente ignorándolo, porque un panel abierto en dos pestañas
     * o un doble clic no son un error que haya que enseñarle a nadie.
     */
    public boolean puedePasarA(OrderStatus siguiente) {
        return siguientesPosibles().contains(siguiente);
    }

    /**
     * A dónde puede ir un pedido desde aquí. Vacío significa estado final.
     *
     * SE PERMITE SALTAR PASOS HACIA ADELANTE, no solo avanzar de uno en uno: preparar y
     * enviar el mismo día es lo normal en una tienda pequeña, y obligar a pulsar
     * "En preparación" antes de "Enviado" sería burocracia sin ninguna ganancia.
     * Lo que se impide es RETROCEDER, que es lo que provocaba correos de más.
     */
    public Set<OrderStatus> siguientesPosibles() {
        return switch (this) {
            // Aún sin cobrar: o entra el pago, o se cancela sin más consecuencias.
            // PAID a mano existe porque un webhook de Stripe puede perderse y hay que
            // poder desatascar el pedido desde el panel.
            case PENDING_PAYMENT -> EnumSet.of(PAID, CANCELLED);

            // Ya hay dinero: cualquier punto más adelante del flujo, o devolverlo.
            case PAID       -> EnumSet.of(PROCESSING, SHIPPED, DELIVERED, REFUNDED);
            case PROCESSING -> EnumSet.of(SHIPPED, DELIVERED, REFUNDED);
            case SHIPPED    -> EnumSet.of(DELIVERED, REFUNDED);

            // Entregado no es final: quedan los 14 días de desistimiento.
            case DELIVERED  -> EnumSet.of(REFUNDED);

            // Finales de verdad. Un pedido cancelado o devuelto no revive: si el cliente
            // vuelve a querer lo mismo, es un pedido nuevo.
            case CANCELLED, REFUNDED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    /** Si ya no hay camino desde aquí. */
    public boolean esFinal() {
        return siguientesPosibles().isEmpty();
    }
}
