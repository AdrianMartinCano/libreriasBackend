package dev.pimon.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.exception.AppException;
import dev.pimon.storage.config.StorageProperties;
import dev.pimon.storage.dto.UploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Gestiona la subida, conversión y eliminación de archivos.
 *
 * Flujo de subida:
 *   1. Valida tipo MIME y tamaño
 *   2. Guarda el archivo original en un directorio temporal
 *   3. Si webp.enabled=true y cwebp está instalado → convierte a WebP
 *   4. Guarda el resultado en upload-dir/subdir/
 *   5. Devuelve la URL pública (base-url + subdir + filename)
 *
 * Uso típico:
 *   UploadResponse r = storageService.upload(file, "products");
 *   producto.setImageUrl(r.url());
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageProperties props;
    private final WebPConverter      webPConverter;

    /**
     * Sube un archivo y lo convierte a WebP si está configurado.
     *
     * @param file   archivo recibido del frontend
     * @param subdir subdirectorio dentro de upload-dir (ej. "products", "users", "cms")
     * @return URL pública del archivo guardado
     */
    public UploadResponse upload(MultipartFile file, String subdir) {
        validateFile(file);

        Path uploadDir = resolveDir(subdir);
        Path tempFile  = null;
        Path finalFile;
        boolean convertedToWebP = false;

        try {
            String originalExt = getExtension(file.getOriginalFilename());
            String uuid        = UUID.randomUUID().toString();

            // Intentar conversión a WebP
            if (props.getWebp().isEnabled() && isImage(file.getContentType())) {
                tempFile = uploadDir.resolve(uuid + "." + originalExt);
                file.transferTo(tempFile);

                Path webpFile = uploadDir.resolve(uuid + ".webp");
                if (webPConverter.convert(tempFile, webpFile, props.getWebp().getQuality())) {
                    finalFile      = webpFile;
                    convertedToWebP = true;
                    deleteSilently(tempFile);
                } else {
                    // cwebp no disponible → usar archivo original
                    finalFile = tempFile;
                    tempFile  = null;  // ya es el final, no borrar
                }
            } else {
                // Archivo no imagen (pdf, etc.) → guardar tal cual
                finalFile = uploadDir.resolve(uuid + "." + originalExt);
                file.transferTo(finalFile);
            }

            String filename = finalFile.getFileName().toString();
            String url      = buildUrl(subdir, filename);

            log.info("Archivo subido: {} → {} ({} KB, WebP: {})",
                    file.getOriginalFilename(), filename,
                    finalFile.toFile().length() / 1024, convertedToWebP);

            return new UploadResponse(
                    url, filename,
                    convertedToWebP ? "webp" : originalExt,
                    finalFile.toFile().length(),
                    convertedToWebP
            );

        } catch (IOException e) {
            deleteSilently(tempFile);
            log.error("Error subiendo archivo: {}", e.getMessage());
            throw AppException.internalError("Error al guardar el archivo: " + e.getMessage());
        }
    }

    /**
     * Elimina un archivo del almacenamiento.
     *
     * @param subdir   subdirectorio (debe coincidir con el de la subida)
     * @param filename nombre del archivo (con extensión)
     */
    public void delete(String subdir, String filename) {
        // Sanitizar para evitar path traversal
        if (filename.contains("..") || filename.contains("/")) {
            throw AppException.badRequest("Nombre de archivo no válido");
        }

        Path file = resolveDir(subdir).resolve(filename);
        if (!Files.exists(file)) {
            throw AppException.notFound("Archivo no encontrado: " + filename);
        }

        try {
            Files.delete(file);
            log.info("Archivo eliminado: {}/{}", subdir, filename);
        } catch (IOException e) {
            throw AppException.internalError("No se pudo eliminar el archivo: " + e.getMessage());
        }
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw AppException.badRequest("El archivo está vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !props.getAllowedTypes().contains(contentType)) {
            throw AppException.badRequest(
                    "Tipo de archivo no permitido: " + contentType +
                    ". Permitidos: " + props.getAllowedTypes());
        }

        long maxBytes = (long) props.getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw AppException.badRequest(
                    "El archivo supera el tamaño máximo de " + props.getMaxSizeMb() + " MB");
        }
    }

    private Path resolveDir(String subdir) {
        Path dir = Path.of(props.getUploadDir()).resolve(sanitize(subdir));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw AppException.internalError("No se pudo crear el directorio: " + dir);
        }
        return dir;
    }

    private String buildUrl(String subdir, String filename) {
        String base = props.getBaseUrl();
        if (!base.endsWith("/")) base += "/";
        return base + sanitize(subdir) + "/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }

    private boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/")
               && !contentType.equals("image/svg+xml");
    }

    private String sanitize(String subdir) {
        return subdir.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
    }

    private void deleteSilently(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }
    }
}
