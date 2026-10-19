package dev.pimon.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Una línea del carrito al ir a pagar.
 *
 * <h2>Aquí NO viaja el precio, y es deliberado</h2>
 *
 * Solo el id y la cantidad. El importe lo pone el servidor leyendo el producto de la base:
 * si el precio lo mandara el cliente, cualquiera compraría por un euro cambiando un número
 * en la consola del navegador.
 *
 * @param opciones lo que el cliente ha elegido para ESTA línea, en pares nombre/valor.
 *
 * <p><b>La librería no sabe qué significan.</b> Aquí caben el color de un filamento, la
 * dedicatoria de un libro o el tono de unas mechas — depende de la tienda. Cada proyecto
 * decide qué mete y qué es válido implementando {@code ValidadorOpciones}; si no
 * implementa ninguno, se aceptan tal cual.</p>
 *
 * <p>POR QUÉ UN MAPA GENÉRICO Y NO VARIANTES DE PRODUCTO: una variante de verdad lleva
 * existencias, precio y fotos propias, y arrastra el catálogo entero. Esto es una opción
 * del encargo, que es lo que necesita una tienda que fabrica bajo demanda. Meter variantes
 * completas complicaría el checkout de todas las webs para servir a ninguna.</p>
 */
public record CheckoutItemRequest(
        @NotBlank String productId,
        @Positive int    quantity,
        Map<String, String> opciones
) {

    /** Compatibilidad: los clientes que no mandan opciones siguen funcionando igual. */
    public CheckoutItemRequest(String productId, int quantity) {
        this(productId, quantity, null);
    }

    public Map<String, String> opcionesOVacio() {
        return opciones == null ? Map.of() : opciones;
    }
}
