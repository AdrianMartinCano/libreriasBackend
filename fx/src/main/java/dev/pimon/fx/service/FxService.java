package dev.pimon.fx.service;

import dev.pimon.fx.config.FxProperties;
import dev.pimon.fx.dto.FxRatesDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mantiene las tasas de cambio en memoria, sembradas desde {@link FxProperties}.
 * Permite consultarlas, actualizarlas manualmente y refrescarlas desde una API externa.
 * Thread-safe: el estado vive en un {@link AtomicReference} inmutable.
 */
@Slf4j
@Service
public class FxService {

    private final FxProperties props;
    private final RestClient http;
    private final AtomicReference<FxRatesDto> snapshot;

    public FxService(FxProperties props) {
        this.props = props;
        this.snapshot = new AtomicReference<>(
                new FxRatesDto(props.getBase(), new LinkedHashMap<>(props.getRates()), Instant.now()));

        // Timeouts explícitos: una API de cambio externa que no responda NO debe bloquear
        // el hilo del scheduler ni el endpoint de refresh manual.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    /** Tasas actuales. */
    public FxRatesDto current() {
        return snapshot.get();
    }

    /** Fusiona/actualiza tasas concretas (mantiene la base y el resto de tasas). */
    public FxRatesDto updateRates(Map<String, Double> rates) {
        FxRatesDto cur = snapshot.get();
        Map<String, Double> merged = new LinkedHashMap<>(cur.rates());
        if (rates != null) merged.putAll(rates);
        FxRatesDto next = new FxRatesDto(cur.base(), merged, Instant.now());
        snapshot.set(next);
        return next;
    }

    /** Reemplaza base + tasas por completo. */
    public FxRatesDto replace(String base, Map<String, Double> rates) {
        FxRatesDto next = new FxRatesDto(
                base != null ? base : snapshot.get().base(),
                rates != null ? new LinkedHashMap<>(rates) : new LinkedHashMap<>(),
                Instant.now());
        snapshot.set(next);
        return next;
    }

    /** Trae tasas frescas de la API externa configurada. Best-effort: conserva las últimas si falla. */
    public FxRatesDto refresh() {
        try {
            String url = props.getRefresh().getUrl() + props.getBase();
            Map<?, ?> body = http.get().uri(url).retrieve().body(Map.class);
            if (body != null && body.get("rates") instanceof Map<?, ?> raw) {
                Map<String, Double> rates = new LinkedHashMap<>();
                raw.forEach((k, v) -> {
                    if (v instanceof Number n) rates.put(String.valueOf(k), n.doubleValue());
                });
                log.info("FX: tasas actualizadas desde {} ({} monedas)", url, rates.size());
                return replace(props.getBase(), rates);
            }
            log.warn("FX: respuesta sin 'rates' desde {}", url);
        } catch (Exception e) {
            log.warn("FX: no se pudieron refrescar las tasas: {}", e.getMessage());
        }
        return snapshot.get();
    }
}
