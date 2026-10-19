package dev.pimon.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que llega en los dos pasos de recuperar una contrasena.
 *
 * Van juntos en un fichero porque son dos mitades de lo mismo y por separado obligan a
 * saltar entre ficheros para entender el flujo.
 */
public final class RecuperacionRequests {

    private RecuperacionRequests() {
    }

    /** Paso 1: «se me ha olvidado». */
    public record Solicitar(
            @NotBlank(message = "Falta el correo")
            @Email(message = "Ese correo no parece valido")
            String email
    ) {
    }

    /**
     * Paso 2: la contrasena nueva, con el token del enlace.
     *
     * El minimo se repite aqui y no se hereda de {@code RegisterRequest} a proposito: si
     * algun dia cambia en uno y no en el otro, un usuario podria acabar con una contrasena
     * que su propio registro habria rechazado.
     */
    public record Restablecer(
            @NotBlank(message = "Falta el token")
            String token,

            @NotBlank(message = "Falta la contrasena")
            @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
            String password
    ) {
    }
}
