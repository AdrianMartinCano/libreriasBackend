package dev.pimon.security.annotation;

import java.lang.annotation.*;

/**
 * Inyecta el AuthUser autenticado en los parámetros de un controller.
 *
 * Uso:
 *   @GetMapping("/perfil")
 *   public ApiResponse<String> perfil(@CurrentUser AuthUser user) {
 *       return ApiResponse.ok("Hola " + user.email() + ", roles: " + user.roles());
 *   }
 *
 * Lanza 401 si la ruta no está en public-paths y el usuario no está autenticado.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
