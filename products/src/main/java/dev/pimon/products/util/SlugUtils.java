package dev.pimon.products.util;

import java.text.Normalizer;

public class SlugUtils {

    private SlugUtils() {}

    /** Convierte "Silla Nórdica!" → "silla-nordica" */
    public static String toSlug(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")          // elimina diacríticos (tildes, ñ...)
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")   // elimina caracteres especiales
                .replaceAll("[\\s-]+", "-")         // espacios → guiones
                .replaceAll("^-+|-+$", "");         // elimina guiones al inicio/final
    }
}
