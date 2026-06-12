package dev.pimon.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción de aplicación con código HTTP y mensaje legible.
 * Lanzar esta excepción en lugar de excepciones genéricas.
 *
 * GlobalExceptionHandler la captura y devuelve ApiResponse.error().
 *
 * Uso:
 *   throw AppException.notFound("Producto no encontrado con id: " + id);
 *   throw AppException.badRequest("El stock no puede ser negativo");
 *   throw new AppException(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_REQUIRED", "Saldo insuficiente");
 */
@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String     code;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code   = code;
    }

    // ── Constructores de conveniencia ── //

    public static AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static AppException forbidden(String message) {
        return new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static AppException internalError(String message) {
        return new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message);
    }
}
