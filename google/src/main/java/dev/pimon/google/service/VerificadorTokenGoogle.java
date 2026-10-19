package dev.pimon.google.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.google.config.GoogleProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Comprueba que un {@code id_token} viene de verdad de Google y es para nosotros.
 *
 * <b>ESTA CLASE ES LA QUE SOSTIENE TODO EL LOGIN CON GOOGLE.</b> Un {@code id_token} es solo
 * un JWT: si se acepta sin verificar del todo, cualquiera puede fabricarse uno con el correo
 * de otra persona y entrar como ella. Las cuatro comprobaciones, y por qué cada una:
 *
 * <ol>
 *   <li><b>Firma</b> — que lo emitió Google. La hace {@link JwtDecoder} contra las claves
 *       públicas que Google publica, y es lo que impide fabricar un token a mano.</li>
 *   <li><b>Caducidad</b> — también la hace el decodificador. Un token robado deja de valer en
 *       cuestión de una hora.</li>
 *   <li><b>{@code aud} = nuestro client_id</b> — que ese token se emitió <b>para esta
 *       aplicación</b>. Sin esto valdría uno emitido para cualquier otra web que use Google:
 *       quien tenga una aplicación registrada podría coger el token que le dan sus propios
 *       usuarios y usarlo aquí para entrar como ellos.</li>
 *   <li><b>{@code email_verified} = true</b> — que Google ha comprobado que ese correo es
 *       suyo. Esta es la que protege las cuentas que <b>ya existen</b>: se vincula por correo,
 *       así que aceptar uno sin verificar permitiría apropiarse de la cuenta de alguien
 *       registrado con contraseña.</li>
 * </ol>
 *
 * Las cuatro son obligatorias. Quitar cualquiera convierte el endpoint en un agujero.
 */
@Slf4j
@Component
public class VerificadorTokenGoogle {

    private final JwtDecoder decodificador;
    private final GoogleProperties props;

    public VerificadorTokenGoogle(JwtDecoder decodificadorDeGoogle, GoogleProperties props) {
        this.decodificador = decodificadorDeGoogle;
        this.props         = props;
    }

    /**
     * Verifica el token y devuelve quién es.
     *
     * @throws AppException si no pasa alguna comprobación, <b>siempre con el mismo mensaje</b>.
     *                      Distinguir «caducado» de «no es para esta app» solo le sirve a quien
     *                      esté probando a ver qué cuela.
     */
    public IdentidadGoogle verificar(String idToken) {
        Jwt token;
        try {
            // Firma, caducidad y emisor. Si algo falla, lanza.
            token = decodificador.decode(idToken);
        } catch (JwtException e) {
            log.warn("id_token de Google rechazado al decodificar: {}", e.getMessage());
            throw noValido();
        }

        if (!token.getAudience().contains(props.clientId())) {
            // Esto no es un usuario equivocandose: es un token emitido para otra aplicacion.
            // Se registra con nivel alto porque no deberia pasar nunca.
            log.warn("id_token de Google con aud ajeno: {}", token.getAudience());
            throw noValido();
        }

        if (!Boolean.TRUE.equals(token.getClaim("email_verified"))) {
            log.warn("id_token de Google con el correo sin verificar; no se vincula");
            throw noValido();
        }

        String sub   = token.getSubject();
        String email = token.getClaimAsString("email");
        if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
            log.warn("id_token de Google sin sub o sin email");
            throw noValido();
        }

        // El nombre es opcional: se puede pedir el scope "profile" y que Google no lo mande
        // igual. Sin el se usa la parte del correo antes de la arroba, que es mejor que dejar
        // la cuenta sin nombre.
        String nombre = token.getClaimAsString("name");
        if (nombre == null || nombre.isBlank()) {
            nombre = email.substring(0, email.indexOf('@'));
        }

        return new IdentidadGoogle(sub, email.trim().toLowerCase(), nombre);
    }

    private static AppException noValido() {
        return AppException.unauthorized(
                "No hemos podido validar tu cuenta de Google. Inténtalo de nuevo.");
    }

    /**
     * Quién es, según Google.
     *
     * @param sub    identificador estable de la persona. Es la clave, no el correo.
     * @param email  su dirección, ya verificada y en minúsculas
     * @param nombre para mostrar
     */
    public record IdentidadGoogle(String sub, String email, String nombre) {
    }
}
