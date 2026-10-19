package dev.pimon.google.dto;

/**
 * Si la cuenta entro por Google.
 *
 * @param vinculada true si existe vinculo con Google, y por tanto lo mas probable es que esa
 *                  persona no tenga ninguna contrasena que conozca
 */
public record VinculoGoogleResponse(boolean vinculada) {
}
