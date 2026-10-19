package dev.pimon.users.event;

/**
 * Alguien ha pedido recuperar su contrasena y hay que hacerle llegar el enlace.
 *
 * <h2>La libreria no manda el correo, y es a proposito</h2>
 *
 * Mismo criterio que {@code PedidoCambioEstado} en {@code spring-ecommerce}: aqui no se
 * sabe como se llama la tienda, en que tono escribe, ni cual es la URL de su pantalla de
 * restablecer. Poniendo el correo dentro habria que hacerlo generico —y sonaria a nada— o
 * llenarlo de huecos configurables hasta que sea peor que escribirlo.
 *
 * Ademas, {@code spring-users} no depende de {@code spring-emails} y asi sigue.
 *
 * <h2>Este evento lleva el token EN CLARO</h2>
 *
 * Es la unica vez que existe fuera del correo del usuario: en la base solo queda su hash.
 * Quien lo escuche debe usarlo para construir el enlace y nada mas — <b>no registrarlo en
 * un log, no guardarlo</b>. Un token en un fichero de log es un token que sigue abriendo
 * la cuenta.
 *
 * <pre>{@code
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * void enviar(RecuperacionSolicitada e) {
 *     String enlace = "https://mitienda.com/restablecer?token=" + e.token();
 *     email.sendTemplate(e.email(), "Recupera tu contraseña", "recuperar",
 *                        Map.of("nombre", e.nombre(), "enlace", enlace));
 * }
 * }</pre>
 *
 * <h2>AFTER_COMMIT, no un @EventListener normal</h2>
 *
 * Se publica dentro de la transaccion que guarda el token. Escuchandolo a secas, el correo
 * podria salir con un enlace que no llega a existir si la transaccion se deshace.
 *
 * @param email  a donde mandarlo
 * @param nombre para poder saludar
 * @param token  el token en claro, solo para construir el enlace
 */
public record RecuperacionSolicitada(String email, String nombre, String token) {

    /**
     * Se sobreescribe para que el token no acabe en un log por accidente.
     *
     * Basta con que alguien escriba {@code log.info("{}", evento)} —o que un manejador de
     * errores registre el evento entero— para dejar un permiso de entrada escrito en un
     * fichero que se rota, se copia y se lee sin pensar.
     */
    @Override
    public String toString() {
        return "RecuperacionSolicitada[email=" + email + ", token=(oculto)]";
    }
}
