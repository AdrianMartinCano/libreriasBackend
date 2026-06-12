package dev.pimon.users.entity;

/**
 * Roles de la aplicación.
 * Se almacenan como String en la tabla user_roles.
 *
 *   ROLE_USER   → usuario estándar (acceso a su propio perfil y recursos)
 *   ROLE_ADMIN  → administrador (acceso total)
 *   ROLE_EDITOR → puede gestionar contenido pero no usuarios
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_EDITOR
}
