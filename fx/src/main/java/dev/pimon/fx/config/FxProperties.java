package dev.pimon.fx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración en application.yml:
 *
 * pimon:
 *   fx:
 *     base: EUR
 *     rates:
 *       USD: 1.16
 *       GBP: 0.86
 *     refresh:
 *       enabled: false          # poner true para refrescar desde una API externa
 *       url: "https://api.frankfurter.app/latest?base="   # sin API key; se le concatena la base
 *       interval-ms: 21600000   # cada 6 horas
 *
 * Las tasas son "1 base = rate X". El frontend (provideLibFormat) las consume y convierte.
 */
@Data
@ConfigurationProperties(prefix = "pimon.fx")
public class FxProperties {

    /** Moneda base de las tasas. */
    private String base = "EUR";

    /** Tasas iniciales por código ISO. Ej: { USD: 1.16, GBP: 0.86 }. */
    private Map<String, Double> rates = new LinkedHashMap<>();

    /** Refresco automático desde una API de cambio externa. */
    private Refresh refresh = new Refresh();

    @Data
    public static class Refresh {
        /** Activa el refresco periódico. Desactivado por defecto (no hace llamadas externas). */
        private boolean enabled = false;

        /** Endpoint que devuelve { base, rates: {...} }. Por defecto frankfurter.app (sin API key). */
        private String url = "https://api.frankfurter.app/latest?base=";

        /** Milisegundos entre refrescos. Por defecto 6 horas. */
        private long intervalMs = 21_600_000L;
    }
}
