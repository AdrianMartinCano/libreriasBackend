package dev.pimon.ecommerce.config;

import dev.pimon.ecommerce.service.CaducidadPedidos;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Programa la revision de pedidos sin pagar.
 *
 * <h2>Apagado por defecto</h2>
 *
 * Igual que {@code FxRefreshConfig}, esto solo existe con
 * {@code pimon.ecommerce.caducidad.enabled = true}. Cancela pedidos y mueve existencias:
 * que se pusiera en marcha solo al actualizar la libreria seria una sorpresa fea en una
 * tienda en funcionamiento.
 *
 * <h2>El {@code @EnableScheduling} va aqui dentro a proposito</h2>
 *
 * Colgado de la condicion, una aplicacion que no use la caducidad no se encuentra el
 * planificador de Spring encendido sin haberlo pedido.
 *
 * <h2>{@code fixedDelay}, no {@code fixedRate}</h2>
 *
 * Cuenta desde que TERMINA la pasada anterior. Con {@code fixedRate}, una revision lenta
 * —muchos pedidos, Stripe tardando— se solaparia con la siguiente y las dos mirarian los
 * mismos pedidos a la vez.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "pimon.ecommerce.caducidad", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class CaducidadPedidosConfig {

    private final CaducidadPedidos caducidad;

    @Scheduled(fixedDelayString = "${pimon.ecommerce.caducidad.intervalo-ms:300000}")
    void revisar() {
        caducidad.caducarPendientes();
    }
}
