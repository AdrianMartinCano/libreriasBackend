package dev.pimon.forum.service;

/**
 * Resuelve el nombre público (nick) de un usuario a partir de su id, en el servidor.
 *
 * La app consumidora puede declarar un bean que implemente esta interfaz (p. ej.
 * consultando spring-users) para que los hilos y mensajes se firmen con el nick
 * actual del perfil — sin que el cliente pueda falsearlo y reflejando en vivo los
 * cambios de nick. Si no hay implementación, el foro cae a la parte local del email.
 */
public interface ForumAuthorNameResolver {
    /** Nombre a mostrar para ese usuario, o null si no se puede resolver. */
    String resolve(String userId);
}
