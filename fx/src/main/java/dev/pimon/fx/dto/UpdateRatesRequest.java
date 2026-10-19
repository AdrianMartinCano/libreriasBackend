package dev.pimon.fx.dto;

import java.util.Map;

/**
 * Cuerpo para actualizar tasas manualmente (endpoint admin).
 *
 * @param base    moneda base; solo se aplica si replace = true
 * @param rates   tasas por código ISO
 * @param replace true = sustituye base + tasas por completo; false = fusiona solo las tasas dadas
 */
public record UpdateRatesRequest(String base, Map<String, Double> rates, boolean replace) {
}
