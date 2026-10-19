package dev.pimon.menu.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Presentación de la carta y el menú del día, editable en caliente desde el panel. Se persiste como
 * JSON en {@code app_settings} bajo la clave "menu"; los defaults salen de {@code pimon.menu.*}.
 *
 * <p>Gobierna carta y menú del día por igual. No incluye imágenes: la decoración es solo tipográfica
 * (fuentes de la whitelist), color de acento e iconos de categoría curados.</p>
 */
public record MenuPresentation(
        @NotNull FontChoice headingFont,
        @NotNull FontChoice bodyFont,

        /** Color de acento en #RRGGBB (validado para no inyectar CSS arbitrario). */
        @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Color HEX #RRGGBB") String accentColor,

        @NotNull MenuLayout layout,
        boolean showPrices,
        boolean showAllergens,

        /** ISO 4217, p. ej. "EUR". */
        @NotBlank String currency
) {}
