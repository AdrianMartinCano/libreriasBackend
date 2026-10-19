package dev.pimon.ecommerce.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las reglas de por donde puede pasar un pedido.
 *
 * <h2>Por que estas reglas merecen tests y no otras</h2>
 *
 * Son cuatro decisiones de negocio disfrazadas de {@code switch}, y las cuatro se pueden
 * «simplificar» sin querer dentro de seis meses porque parecen arbitrarias. No lo son:
 *
 * <ul>
 *   <li>Se puede saltar hacia adelante — hay piezas que se imprimen y se mandan el mismo dia.</li>
 *   <li>Cancelar solo antes de cobrar; despues es reembolsar, que mueve dinero de verdad.</li>
 *   <li>Entregado NO es final: quedan los 14 dias de desistimiento.</li>
 *   <li>Nunca se va hacia atras.</li>
 * </ul>
 *
 * Cada test de aqui dice en su nombre cual de esas reglas protege, para que quien lo rompa
 * lea el motivo antes de «arreglarlo».
 */
class OrderStatusTest {

    @Nested
    @DisplayName("sin cobrar")
    class SinCobrar {

        @Test
        @DisplayName("puede pagarse o cancelarse, y nada mas")
        void desdePendienteDePago() {
            assertThat(OrderStatus.PENDING_PAYMENT.siguientesPosibles())
                    .containsExactlyInAnyOrder(OrderStatus.PAID, OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("no se puede enviar algo que no se ha cobrado")
        void noSePuedeEnviarSinCobrar() {
            assertThat(OrderStatus.PENDING_PAYMENT.puedePasarA(OrderStatus.SHIPPED)).isFalse();
        }

        @Test
        @DisplayName("se puede marcar PAID a mano: un webhook de Stripe se puede perder")
        void seDesatascaAMano() {
            // Esto no es un atajo: es la salida cuando el webhook no llega y el pedido se
            // queda colgado con el dinero ya cobrado. Nos paso.
            assertThat(OrderStatus.PENDING_PAYMENT.puedePasarA(OrderStatus.PAID)).isTrue();
        }
    }

    @Nested
    @DisplayName("ya cobrado")
    class Cobrado {

        @Test
        @DisplayName("se puede saltar hacia adelante: hay piezas que se imprimen y se mandan el mismo dia")
        void sePuedeSaltarPasos() {
            assertThat(OrderStatus.PAID.puedePasarA(OrderStatus.SHIPPED)).isTrue();
            assertThat(OrderStatus.PAID.puedePasarA(OrderStatus.DELIVERED)).isTrue();
        }

        @Test
        @DisplayName("NO se puede cancelar: con dinero de por medio se reembolsa")
        void cobradoNoSeCancela() {
            // Cancelar y reembolsar no son sinonimos: uno no mueve dinero y el otro si.
            // Permitir CANCELLED aqui dejaria pedidos cerrados con el cobro sin devolver.
            assertThat(OrderStatus.PAID.puedePasarA(OrderStatus.CANCELLED)).isFalse();
            assertThat(OrderStatus.PAID.puedePasarA(OrderStatus.REFUNDED)).isTrue();
        }

        @Test
        @DisplayName("no se vuelve atras")
        void noSeVuelveAtras() {
            assertThat(OrderStatus.SHIPPED.puedePasarA(OrderStatus.PROCESSING)).isFalse();
            assertThat(OrderStatus.DELIVERED.puedePasarA(OrderStatus.SHIPPED)).isFalse();
        }
    }

    @Nested
    @DisplayName("finales")
    class Finales {

        @Test
        @DisplayName("entregado NO es final: quedan los 14 dias de desistimiento")
        void entregadoAdmiteReembolso() {
            assertThat(OrderStatus.DELIVERED.esFinal()).isFalse();
            assertThat(OrderStatus.DELIVERED.puedePasarA(OrderStatus.REFUNDED)).isTrue();
        }

        @Test
        @DisplayName("cancelado y reembolsado no reviven")
        void losFinalesSonFinales() {
            // Si el cliente vuelve a querer lo mismo, es un pedido nuevo. Revivir uno
            // cerrado dejaria el historico mintiendo sobre lo que paso.
            assertThat(OrderStatus.CANCELLED.esFinal()).isTrue();
            assertThat(OrderStatus.REFUNDED.esFinal()).isTrue();
        }
    }
}
