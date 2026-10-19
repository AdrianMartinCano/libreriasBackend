package dev.pimon.google.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * El id_token que Google le ha dado al navegador.
 *
 * @param credential se llama asi, y no "token", porque es el nombre exacto del campo que
 *                   devuelve la libreria de Google en el front. Renombrarlo aqui solo
 *                   anadiria una traduccion que hay que recordar en los dos lados.
 */
public record LoginGoogleRequest(
        @NotBlank(message = "Falta la credencial de Google")
        String credential
) {
}
