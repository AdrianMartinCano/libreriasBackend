package dev.pimon.storage.dto;

public record UploadResponse(
        String   url,           // URL pública para usar en imageUrl de productos, etc.
        String   filename,      // nombre del archivo guardado
        String   format,        // "webp" o el formato original
        long     sizeBytes,     // tamaño final del archivo guardado
        boolean  convertedToWebP
) {}
