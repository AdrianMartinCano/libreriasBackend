package dev.pimon.blog.service;

/**
 * Resuelve el nombre público (nick) de un usuario a partir de su id, en el servidor.
 *
 * La app consumidora puede declarar un bean que implemente esta interfaz (p. ej.
 * consultando spring-users) para que los comentarios se firmen con el nick del
 * perfil — sin que el cliente pueda falsearlo. Si no hay implementación, el blog
 * usa la parte local del email.
 */
public interface CommentAuthorNameResolver {
    /** Nombre a mostrar para ese usuario, o null si no se puede resolver. */
    String resolve(String userId);
}
