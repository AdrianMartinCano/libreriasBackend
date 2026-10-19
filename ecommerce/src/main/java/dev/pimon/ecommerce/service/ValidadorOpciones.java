package dev.pimon.ecommerce.service;

import dev.pimon.products.entity.Product;

import java.util.Map;

/**
 * Comprueba que las opciones elegidas para una línea son válidas <b>para esta tienda</b>.
 *
 * <h2>Por qué esto es un punto de extensión y no código de la librería</h2>
 *
 * La librería no puede saber qué significan las opciones. En pimon3d son colores de
 * filamento que hay que tener en stock; en una librería podrían ser una dedicatoria con un
 * máximo de caracteres; en una peluquería, el tono de unas mechas. Cada tienda tiene sus
 * reglas y sus listas.
 *
 * Mismo patrón que {@link CalculadorEnvio}: la librería define el hueco y el proyecto lo
 * rellena con un {@code @Bean}. Si no hay ninguno, se aceptan las opciones tal cual —una
 * tienda que no las use no tiene que implementar nada.
 *
 * <h2>Esto se ejecuta EN EL SERVIDOR y no es opcional</h2>
 *
 * El desplegable de la web solo enseña lo válido, pero la petición se puede construir a
 * mano. Sin esta comprobación entraría un pedido con un color retirado, y el fallo
 * aparecería al ir a imprimirlo.
 */
@FunctionalInterface
public interface ValidadorOpciones {

    /**
     * @param producto el que se está comprando, por si las opciones válidas dependen de él
     * @param opciones lo que ha elegido el cliente
     * @throws dev.pimon.common.exception.AppException si algo no vale, con un mensaje que
     *         se le pueda enseñar a quien está comprando
     */
    void validar(Product producto, Map<String, String> opciones);
}
