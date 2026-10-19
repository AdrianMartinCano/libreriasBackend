package dev.pimon.common.exception;

import dev.pimon.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Captura las excepciones de todos los controllers y devuelve respuestas
 * {@link ApiResponse} estandarizadas, con el código HTTP correcto y sin filtrar
 * detalles internos.
 *
 * <p>Habilitado automáticamente por {@code CommonAutoConfiguration}.</p>
 *
 * <h2>Mapa de excepciones → HTTP</h2>
 * <ul>
 *   <li>{@link AppException} → su propio status (incluye {@code code})</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 ({@code @Valid} en el body)</li>
 *   <li>{@link ConstraintViolationException} → 400 ({@code @Validated} en params/path)</li>
 *   <li>{@link MissingServletRequestParameterException} → 400 (falta query param)</li>
 *   <li>{@link MethodArgumentTypeMismatchException} → 400 (tipo de parámetro incorrecto)</li>
 *   <li>{@link HttpMessageNotReadableException} → 400 (JSON ausente o mal formado)</li>
 *   <li>{@link DataIntegrityViolationException} → 409 (unique/FK/not-null en BD)</li>
 *   <li>{@link MaxUploadSizeExceededException} → 413 (archivo demasiado grande)</li>
 *   <li>{@link NoResourceFoundException} → 404 (recurso estático inexistente)</li>
 *   <li>{@link Exception} → 500 (cualquier otro; se loguea completo)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Excepciones de negocio lanzadas con AppException. Propaga el {@code code} al cliente. */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        log.warn("[{}] {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getCode(), null));
    }

    /** Errores de validación de {@code @Valid} en el cuerpo de la petición. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                // si un campo acumula varios errores, conservamos el primero (mensaje más relevante)
                fieldErrors.merge(error.getField(), msg(error), (first, next) -> first));

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Error de validación", "VALIDATION_ERROR", fieldErrors));
    }

    /** Violaciones de constraints en parámetros/path con {@code @Validated} (no en el body). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> violations = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            // dejamos solo el último segmento del path (ej. "listar.size" → "size")
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            violations.merge(field, v.getMessage(), (first, next) -> first);
        });

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Error de validación", "VALIDATION_ERROR", violations));
    }

    /** Falta un parámetro de query obligatorio. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Falta el parámetro obligatorio: " + ex.getParameterName(),
                        "MISSING_PARAMETER", null));
    }

    /** Tipo de parámetro incorrecto en la URL. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Parámetro inválido: " + ex.getName(), "BAD_REQUEST", null));
    }

    /** Cuerpo JSON ausente o mal formado (antes caía a 500). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Cuerpo de la petición ilegible: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("El cuerpo de la petición está ausente o mal formado.",
                        "MALFORMED_REQUEST", null));
    }

    /** Conflictos de integridad en BD: unique, clave foránea, not-null (antes caía a 500). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Logueamos el detalle real (puede contener nombres de columnas/constraints) pero NO lo exponemos.
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "La operación entra en conflicto con datos existentes.", "DATA_CONFLICT", null));
    }

    /** Archivo subido por encima del límite configurado. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("El archivo supera el tamaño máximo permitido.",
                        "FILE_TOO_LARGE", null));
    }

    /** Recurso estático no encontrado (archivos en /uploads, etc.) → 404 limpio. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Recurso no encontrado: " + ex.getResourcePath(),
                        "NOT_FOUND", null));
    }

    /** Cualquier otro error no controlado. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Error no controlado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Ha ocurrido un error interno. Por favor, inténtalo de nuevo.",
                        "INTERNAL_ERROR", null));
    }

    private static String msg(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "valor no válido";
    }
}
