package dev.pimon.security.model;

import java.util.Collection;

/**
 * Usuario autenticado extraído del JWT.
 * Se inyecta en los controllers con @CurrentUser.
 *
 * Uso:
 *   @GetMapping("/perfil")
 *   public ApiResponse<String> perfil(@CurrentUser AuthUser user) {
 *       return ApiResponse.ok("Hola " + user.email());
 *   }
 */
public record AuthUser(
        String             id,
        String             email,
        Collection<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
