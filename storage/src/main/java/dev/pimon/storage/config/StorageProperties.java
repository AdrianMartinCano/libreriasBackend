package dev.pimon.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración en application.yml:
 *
 * libui:
 *   storage:
 *     upload-dir: /var/uploads          # directorio en el servidor
 *     base-url: https://tudominio.com/uploads  # URL que sirve Nginx
 *     max-size-mb: 10
 *     allowed-types:
 *       - image/jpeg
 *       - image/png
 *       - image/gif
 *       - image/webp
 *     webp:
 *       enabled: true    # convierte automáticamente a WebP
 *       quality: 85      # calidad 0-100 (80-90 es buen balance)
 */
@Data
@ConfigurationProperties(prefix = "pimon.storage")
public class StorageProperties {

    /** Directorio raíz donde se guardan los archivos */
    private String uploadDir = "./uploads";

    /** URL base pública (la que sirve Nginx o Spring) */
    private String baseUrl = "http://localhost:8080/uploads";

    private int maxSizeMb = 10;

    private List<String> allowedTypes = List.of(
            "image/jpeg", "image/png", "image/gif",
            "image/webp", "image/svg+xml",
            "image/heic", "image/heif"
    );

    private WebP webp = new WebP();

    @Data
    public static class WebP {
        /** Activa la conversión automática a WebP */
        private boolean enabled = true;

        /** Calidad de compresión WebP (0-100). 85 = excelente balance */
        private int quality = 85;

        /** Ruta al ejecutable cwebp. Por defecto busca en el PATH del sistema */
        private String cwebpPath = "cwebp";
    }
}
