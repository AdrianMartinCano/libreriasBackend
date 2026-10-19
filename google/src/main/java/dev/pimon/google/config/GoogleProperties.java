package dev.pimon.google.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuración del login con Google.
 *
 * <h2>Está apagado por defecto</h2>
 *
 * Con {@code habilitado = false} —lo normal— {@link GoogleAutoConfiguration} no carga nada:
 * ni el endpoint, ni la entidad del vínculo, ni el decodificador. Así se puede tener el
 * módulo en el {@code pom} y arrancar en local sin configurar una cuenta de Google, y una
 * aplicación con {@code ddl-auto: validate} no exige una tabla que todavía no usa.
 *
 * <h2>El client-id es obligatorio si está encendido</h2>
 *
 * No es un secreto —viaja en el HTML del front— pero sí es imprescindible: es contra lo que
 * se comprueba el {@code aud} del token. Sin esa comprobación valdría un token emitido para
 * <b>cualquier otra</b> aplicación que use Google, y el login se convertiría en una puerta
 * trasera al login normal. Por eso {@code @NotBlank}: mejor no arrancar que arrancar con el
 * agujero abierto.
 *
 * @param habilitado si hay login con Google. Enciéndelo solo cuando tengas client-id.
 * @param clientId   el ID de cliente de OAuth (termina en {@code .apps.googleusercontent.com})
 * @param emisores   quién puede haber firmado el token. Google usa las dos formas del mismo
 *                   emisor según el flujo, y las dos son legítimas.
 */
@Validated
@ConfigurationProperties(prefix = "pimon.google")
public record GoogleProperties(
        @DefaultValue("false") boolean habilitado,

        @NotBlank(message = "pimon.google.client-id es obligatorio si el login con Google está habilitado")
        @DefaultValue("") String clientId,

        @DefaultValue({"https://accounts.google.com", "accounts.google.com"}) List<String> emisores
) {
}
