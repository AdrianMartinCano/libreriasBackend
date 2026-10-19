package dev.pimon.fx.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.fx.dto.FxRatesDto;
import dev.pimon.fx.service.FxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público de tasas de cambio.
 * Añade /api/fx a pimon.security.public-paths para que sea accesible sin token.
 */
@RestController
@RequestMapping("/api/fx")
@RequiredArgsConstructor
public class FxController {

    private final FxService service;

    /** Devuelve la moneda base y las tasas actuales. */
    @GetMapping
    public ResponseEntity<ApiResponse<FxRatesDto>> rates() {
        return ResponseEntity.ok(ApiResponse.ok(service.current()));
    }
}
