package dev.pimon.fx.config;

import dev.pimon.fx.service.FxService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Refresco periódico de tasas desde la API externa.
 * Solo se activa con pimon.fx.refresh.enabled = true (por defecto, off → sin llamadas externas).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "pimon.fx.refresh", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class FxRefreshConfig {

    private final FxService fx;

    @Scheduled(fixedDelayString = "${pimon.fx.refresh.interval-ms:21600000}")
    void scheduledRefresh() {
        fx.refresh();
    }
}
