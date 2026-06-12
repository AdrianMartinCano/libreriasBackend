package dev.pimon.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.storage.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Convierte imágenes a WebP usando el ejecutable cwebp de Google.
 *
 * Instalación en Ubuntu/Debian:
 *   apt install webp
 *
 * Instalación en macOS:
 *   brew install webp
 *
 * Si cwebp no está disponible, isAvailable() devuelve false y
 * StorageService guarda el archivo en su formato original.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebPConverter {

    private final StorageProperties props;

    private Boolean cwebpAvailable = null;  // cache del check

    /**
     * Convierte un archivo de imagen a WebP.
     *
     * @param input   archivo de origen (jpg, png, gif...)
     * @param output  archivo de destino (.webp)
     * @param quality calidad 0-100
     * @return true si la conversión fue exitosa
     */
    public boolean convert(Path input, Path output, int quality) {
        if (!isAvailable()) {
            log.debug("cwebp no disponible — saltando conversión WebP");
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    props.getWebp().getCwebpPath(),
                    "-q", String.valueOf(quality),
                    "-quiet",
                    input.toAbsolutePath().toString(),
                    "-o", output.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("cwebp tardó más de 30s — forzando cierre");
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                long originalSize = input.toFile().length();
                long webpSize     = output.toFile().length();
                int  savings      = (int) ((1 - (double) webpSize / originalSize) * 100);
                log.info("WebP: {} → {} ({} KB → {} KB, ahorro {}%)",
                        input.getFileName(), output.getFileName(),
                        originalSize / 1024, webpSize / 1024, savings);
                return true;
            } else {
                log.warn("cwebp terminó con código {}", exitCode);
                return false;
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error ejecutando cwebp: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Verifica si cwebp está instalado en el sistema.
     * El resultado se cachea — solo se comprueba una vez al arrancar.
     */
    public boolean isAvailable() {
        if (cwebpAvailable != null) return cwebpAvailable;

        try {
            Process p = new ProcessBuilder(props.getWebp().getCwebpPath(), "-version")
                    .redirectErrorStream(true)
                    .start();
            p.waitFor(5, TimeUnit.SECONDS);
            cwebpAvailable = p.exitValue() == 0;
            if (cwebpAvailable) {
                log.info("cwebp disponible — conversión WebP activada");
            } else {
                log.warn("cwebp no disponible — las imágenes se guardarán en formato original");
            }
        } catch (Exception e) {
            cwebpAvailable = false;
            log.warn("cwebp no encontrado ({}). Instalar con: apt install webp", e.getMessage());
        }
        return cwebpAvailable;
    }
}
