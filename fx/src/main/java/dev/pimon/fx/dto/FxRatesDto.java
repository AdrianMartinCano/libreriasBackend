package dev.pimon.fx.dto;

import java.time.Instant;
import java.util.Map;

/** Tasas de cambio servidas por la API: moneda base, mapa de tasas y momento de actualización. */
public record FxRatesDto(String base, Map<String, Double> rates, Instant updatedAt) {
}
