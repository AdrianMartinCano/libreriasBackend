package dev.pimon.menu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Valores por defecto de la presentación de la carta/menú, en application.yml:
 *
 * pimon:
 *   menu:
 *     heading-font: SERIF
 *     body-font: SANS
 *     accent-color: "#8a5a2b"
 *     layout: LIST
 *     currency: EUR
 *
 * Son solo los defaults: la presentación es editable en caliente desde el panel y se persiste
 * vía {@code SettingsStore} (clave "menu").
 */
@Data
@ConfigurationProperties(prefix = "pimon.menu")
public class MenuProperties {

    /** Fuente de títulos/secciones (nombre de la whitelist, no CSS libre). */
    private String headingFont = "SERIF";

    /** Fuente del cuerpo (descripciones de platos). */
    private String bodyFont = "SANS";

    /** Color de acento en #RRGGBB. */
    private String accentColor = "#8a5a2b";

    /** Disposición de la carta: LIST | CARDS | COLUMNS. */
    private String layout = "LIST";

    /** Mostrar precios en la carta pública. */
    private boolean showPrices = true;

    /** Mostrar la leyenda de alérgenos. */
    private boolean showAllergens = true;

    /** Código ISO 4217 de la moneda con la que se formatean los precios. */
    private String currency = "EUR";
}
