package dev.pimon.dining.settings;

import dev.pimon.common.settings.SettingsStore;
import dev.pimon.dining.config.DiningProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Acceso a los {@link DiningSettings} vigentes (cacheados por {@link SettingsStore}). Si no hay fila
 * en BD, devuelve los valores por defecto derivados de {@code application.yml} ({@code pimon.dining.*}).
 */
@Service
@RequiredArgsConstructor
public class DiningSettingsService {

    static final String KEY = "dining";

    private final SettingsStore    store;
    private final DiningProperties props;

    public DiningSettings current() {
        return store.get(KEY, DiningSettings.class, this::defaults);
    }

    public DiningSettings update(DiningSettings settings) {
        return store.save(KEY, settings);
    }

    private DiningSettings defaults() {
        return new DiningSettings(
                props.getTotalCapacity(),
                props.getMaxAdvanceDays(),
                props.getMinAdvanceHours(),
                props.getBlockedDates());
    }
}
