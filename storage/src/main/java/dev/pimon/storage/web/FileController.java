package dev.pimon.storage.web;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.storage.dto.UploadResponse;
import dev.pimon.storage.service.StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de gestión de archivos.
 *
 * POST /api/files/upload?subdir=products  → sube y convierte a WebP
 * DELETE /api/admin/files/{subdir}/{filename} → elimina un archivo
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    /**
     * Sube un archivo.
     *
     * @param file   multipart/form-data — campo "file"
     * @param subdir subdirectorio de destino: products, users, cms, misc
     */
    @PostMapping(value = "/api/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "misc") String subdir) {

        UploadResponse response = storageService.upload(file, subdir);

        String message = response.convertedToWebP()
                ? "Imagen subida y convertida a WebP (" + response.sizeBytes() / 1024 + " KB)"
                : "Archivo subido correctamente (" + response.sizeBytes() / 1024 + " KB)";

        return ResponseEntity.ok(ApiResponse.ok(response, message));
    }

    /**
     * Elimina un archivo (solo ADMIN).
     *
     * @param subdir   subdirectorio donde está el archivo
     * @param filename nombre del archivo con extensión
     */
    @DeleteMapping("/api/admin/files/{subdir}/{filename}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String subdir,
            @PathVariable String filename) {
        storageService.delete(subdir, filename);
        return ResponseEntity.ok(ApiResponse.noContent("Archivo eliminado: " + filename));
    }
}
