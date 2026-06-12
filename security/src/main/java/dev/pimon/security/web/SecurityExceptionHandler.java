package dev.pimon.security.web;

import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.dto.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Captura excepciones de seguridad que ocurren DENTRO del controller
 * (principalmente @PreAuthorize y @Secured en Spring Security 6.x).
 *
 * @Order(HIGHEST_PRECEDENCE) garantiza que este handler se evalúa ANTES
 * que GlobalExceptionHandler, que captura Exception genérico.
 *
 * Diferencia con accessDeniedHandler en SecurityConfig:
 *   accessDeniedHandler  → rutas bloqueadas por el filtro HTTP (authorizeHttpRequests)
 *   SecurityExceptionHandler → métodos bloqueados por @PreAuthorize (dentro del controller)
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(
            AuthorizationDeniedException ex) {
        log.warn("Acceso denegado por @PreAuthorize: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Acceso denegado — no tienes permisos para este recurso"));
    }
}
