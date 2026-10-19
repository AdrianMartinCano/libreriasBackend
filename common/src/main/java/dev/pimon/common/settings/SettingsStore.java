package dev.pimon.common.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pimon.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Almacén genérico de ajustes editables en caliente (una fila JSON por módulo en {@code app_settings}).
 *
 * <p>Patrón de uso por cada módulo:</p>
 * <pre>
 *   ReservationsSettings s = store.get("reservations", ReservationsSettings.class, this::defaults);
 *   store.save("reservations", nuevosAjustes);
 * </pre>
 *
 * <p><b>Eficiencia:</b> el valor se cachea en memoria; no se consulta la BD en cada lectura. La caché
 * se actualiza al guardar. {@code application.yml} sigue aportando los <b>valores por defecto</b> (vía
 * el {@code Supplier} de {@code get}) cuando todavía no hay fila en la tabla.</p>
 *
 * <p><b>Multi-instancia:</b> la caché es por nodo. En despliegues de una sola instancia (lo habitual)
 * es exacta; con varios nodos, un cambio en uno no invalida la caché de otro al instante — usar
 * {@link #evict(String)} (p. ej. con un TTL o un evento) si se escala horizontalmente.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsStore {

    private final AppSettingRepository repo;
    private final ObjectMapper mapper;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    /**
     * Devuelve los ajustes del módulo, cacheados. Si no hay fila (o el JSON guardado es ilegible),
     * usa los valores por defecto del {@code Supplier} (típicamente derivados de {@code application.yml}).
     */
    public <T> T get(String moduleKey, Class<T> type, Supplier<T> defaults) {
        Object cached = cache.get(moduleKey);
        if (cached != null) {
            return type.cast(cached);
        }
        T value = repo.findById(moduleKey)
                .map(s -> tryDeserialize(s.getPayload(), type))
                .orElse(null);
        if (value == null) {
            value = defaults.get();
        }
        cache.put(moduleKey, value);
        return value;
    }

    /** Persiste los ajustes del módulo (upsert) y refresca la caché. */
    @Transactional
    public <T> T save(String moduleKey, T value) {
        AppSetting setting = repo.findById(moduleKey).orElseGet(() -> new AppSetting(moduleKey));
        setting.setPayload(serialize(value));
        setting.setUpdatedAt(LocalDateTime.now());
        repo.save(setting);
        cache.put(moduleKey, value);
        return value;
    }

    /** Descarta la entrada cacheada de un módulo (para recargar desde BD en multi-instancia). */
    public void evict(String moduleKey) {
        cache.remove(moduleKey);
    }

    private <T> T tryDeserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Ajustes de '{}' ilegibles ({}). Se usarán los valores por defecto.",
                    type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw AppException.internalError("No se pudieron serializar los ajustes: " + e.getMessage());
        }
    }
}
