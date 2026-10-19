package dev.pimon.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.common.exception.AppException;
import dev.pimon.storage.config.StorageProperties;
import dev.pimon.storage.dto.UploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

            // Intentar conversión a WebP.
            //
            // LO QUE YA ES WEBP NO SE CONVIERTE. Convertirlo hacía dos destrozos a la vez,
            // porque el temporal y el destino caían en el MISMO nombre (uuid.webp):
            // cwebp abría el destino para escribir y con eso vaciaba su propia entrada, y
            // si aun así devolvía éxito, el deleteSilently(tempFile) de después borraba el
            // fichero recién escrito. El resultado era una imagen rota o un 404.
            // Y ni siquiera hacía falta: ya está en el formato de destino, y volver a
            // comprimir algo que ya venía comprimido con pérdida solo lo empeora.
            boolean yaEsWebP = "image/webp".equalsIgnoreCase(file.getContentType())
                               || "webp".equals(originalExt);

            if (props.getWebp().isEnabled() && isImage(file.getContentType()) && !yaEsWebP) {
                // EL TEMPORAL LLEVA SUFIJO para no poder coincidir nunca con el destino,
                // pase lo que pase con la extensión de origen.
                tempFile = uploadDir.resolve(uuid + "-origen." + originalExt);
                file.transferTo(tempFile);

                Path webpFile = uploadDir.resolve(uuid + ".webp");
                if (webPConverter.convert(tempFile, webpFile, props.getWebp().getQuality())) {
                    finalFile      = webpFile;
                    convertedToWebP = true;
                    deleteSilently(tempFile);
                    tempFile = null;
                } else {
                    // cwebp no disponible → se queda el original, pero con su nombre
                    // definitivo: nadie tiene por qué ver un «-origen» en una URL pública.
                    finalFile = uploadDir.resolve(uuid + "." + originalExt);
                    Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
                    tempFile = null;
                }
            } else {
                // Lo que no se convierte —un pdf, o una imagen que ya es WebP— se guarda
                // tal cual, con su propia extensión.
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

        validateMagicBytes(file, contentType);
    }

    /**
     * Valida la <b>firma binaria real</b> (magic bytes) del archivo. El {@code Content-Type} lo envía
     * el cliente y es falsificable; aquí confirmamos que un archivo declarado como imagen lo es de
     * verdad antes de guardarlo o pasarlo a cwebp.
     */
    private void validateMagicBytes(MultipartFile file, String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) return;

        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw AppException.badRequest("No se pudo leer el archivo");
        }
        if (read < 4 || !isKnownImage(head)) {
            throw AppException.badRequest("El contenido del archivo no coincide con una imagen válida");
        }
    }

    private boolean isKnownImage(byte[] h) {
        // JPEG: FF D8 FF
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return true;
        // PNG: 89 'P' 'N' 'G'
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return true;
        // GIF: "GIF8"
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return true;
        // WEBP: "RIFF"...."WEBP"
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return true;
        // HEIC/HEIF (y otros ISO-BMFF): caja "ftyp" en el offset 4
        return h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p';
    }

    private Path resolveDir(String subdir) {
        // Ruta absoluta y normalizada: colapsa cualquier ".." (defensa en profundidad junto al
        // saneo de subdir/extensión) y evita ambigüedades de transferTo con rutas relativas.
        Path dir = Path.of(props.getUploadDir()).resolve(sanitize(subdir)).toAbsolutePath().normalize();
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

    /**
     * Extrae la extensión del nombre de archivo del usuario y la <b>sanea</b>: solo se
     * conservan caracteres alfanuméricos. Imprescindible porque la extensión se concatena a
     * la ruta de guardado — sin sanear, un nombre como {@code "x.png/../../etc/passwd"}
     * permitiría un path traversal. Se limita además su longitud.
     */
    private String getExtension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        String ext = (dot >= 0 ? filename.substring(dot + 1) : "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        if (ext.isBlank()) return "bin";
        return ext.length() > 5 ? ext.substring(0, 5) : ext;
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
