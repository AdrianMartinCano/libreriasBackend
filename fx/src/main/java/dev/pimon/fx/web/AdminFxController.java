package dev.pimon.fx.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.fx.dto.FxRatesDto;
import dev.pimon.fx.dto.UpdateRatesRequest;
import dev.pimon.fx.service.FxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Gestión de tasas para administradores. */
@RestController
@RequestMapping("/api/admin/fx")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminFxController {

    private final FxService service;

    /** Actualiza las tasas manualmente (fusiona, o reemplaza si replace = true). */
    @PutMapping
    public ResponseEntity<ApiResponse<FxRatesDto>> update(@RequestBody UpdateRatesRequest req) {
        FxRatesDto updated = req.replace()
                ? service.replace(req.base(), req.rates())
                : service.updateRates(req.rates());
        return ResponseEntity.ok(ApiResponse.ok(updated, "Tasas actualizadas"));
    }

    /** Fuerza un refresco desde la API externa configurada (pimon.fx.refresh.url). */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<FxRatesDto>> refresh() {
        return ResponseEntity.ok(ApiResponse.ok(service.refresh(), "Tasas refrescadas"));
    }
}
