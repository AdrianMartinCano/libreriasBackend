package dev.pimon.reservations.settings;

import dev.pimon.common.exception.AppException;
import dev.pimon.common.settings.SettingsStore;
import dev.pimon.reservations.config.ReservationsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a los {@link ReservationsSettings} vigentes (cacheados por {@link SettingsStore}).
 * Si no hay fila en BD, devuelve los valores por defecto derivados de {@code application.yml}.
 */
@Service
@RequiredArgsConstructor
public class ReservationsSettingsService {

    static final String KEY = "reservations";

    private final SettingsStore store;
    private final ReservationsProperties props;

    public ReservationsSettings current() {
        return store.get(KEY, ReservationsSettings.class, this::defaults);
    }

    public ReservationsSettings update(ReservationsSettings settings) {
        validate(settings);
        return store.save(KEY, settings);
    }

    /** Valida que cada tramo tenga fin posterior al inicio (lo que {@code @Pattern} no puede comprobar). */
    private void validate(ReservationsSettings s) {
        if (s.weeklyHours() == null) return;
        s.weeklyHours().forEach((day, ranges) -> {
            if (ranges == null) return;
            for (TimeRange r : ranges) {
                if (!LocalTime.parse(r.start()).isBefore(LocalTime.parse(r.end()))) {
                    throw AppException.badRequest(
                            "Tramo inválido en " + day + ": la hora de fin debe ser posterior al inicio (" +
                            r.start() + "–" + r.end() + ")");
                }
            }
        });
    }

    /** Por defecto: cada día laborable del yaml con un único tramo [work-start, work-end]. */
    private ReservationsSettings defaults() {
        Map<DayOfWeek, List<TimeRange>> weekly = new LinkedHashMap<>();
        TimeRange fullDay = new TimeRange(props.getWorkStart(), props.getWorkEnd());
        for (String day : props.getWorkDays()) {
            weekly.put(DayOfWeek.valueOf(day), List.of(fullDay));
        }
        return new ReservationsSettings(
                weekly,
                props.getSlotIntervalMinutes(),
                props.getCapacity(),
                props.getBlockedDates());
    }
}
