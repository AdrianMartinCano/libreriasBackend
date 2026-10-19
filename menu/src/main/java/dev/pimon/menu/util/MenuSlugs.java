package dev.pimon.menu.util;

import java.text.Normalizer;

/** Generación de slugs para categorías de la carta. Propio del módulo (sin depender de products). */
public final class MenuSlugs {

    private MenuSlugs() {}

    /** "Postres Caseros!" → "postres-caseros" */
    public static String toSlug(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")          // diacríticos (tildes, ñ...)
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")   // caracteres especiales
                .replaceAll("[\\s-]+", "-")         // espacios → guiones
                .replaceAll("^-+|-+$", "");         // guiones sobrantes
    }
}
