package dev.pimon.google.dto;

/**
 * Lo que el front necesita para pintar el boton de Google.
 *
 * El client_id se sirve desde aqui en lugar de escribirlo en el front por dos motivos: hay
 * uno distinto en local y en produccion, y asi solo existe en un sitio. No es un secreto
 * —acaba en el HTML de todas formas— pero tenerlo duplicado es una fuente de "en mi maquina
 * funciona".
 *
 * @param enabled    siempre true: si el modulo esta apagado, este endpoint no existe y
 *                   responde 404, que es la otra forma de decir que no hay login con Google
 * @param clientId   el ID de cliente de OAuth
 */
public record ConfigGoogleResponse(boolean enabled, String clientId) {
}
