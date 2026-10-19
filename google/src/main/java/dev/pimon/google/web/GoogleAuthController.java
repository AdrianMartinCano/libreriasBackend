package dev.pimon.google.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.google.config.GoogleProperties;
import dev.pimon.google.dto.ConfigGoogleResponse;
import dev.pimon.google.dto.LoginGoogleRequest;
import dev.pimon.google.dto.VinculoGoogleResponse;
import dev.pimon.google.service.GoogleAuthService;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.dto.LoginResponse;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrar con Google.
 *
 * Cuelga de {@code /api/auth/**}, que ya suele ser público: quien va a identificarse, por
 * definición, todavía no está identificado. <b>Cada aplicación tiene que dejar
 * {@code /api/auth/google} en sus {@code public-paths}</b>, o el login responderá 401 antes de
 * llegar aquí.
 */
@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    private final GoogleAuthService servicio;
    private final GoogleProperties  props;

    public GoogleAuthController(GoogleAuthService servicio, GoogleProperties props) {
        this.servicio = servicio;
        this.props    = props;
    }

    /**
     * Con qué client_id hay que pintar el botón.
     *
     * El front lo consulta al cargar para decidir si lo pinta. Con el módulo apagado este
     * endpoint <b>no existe</b> y responde 404: en local, y en cualquier web que no use Google,
     * el botón sencillamente no aparece en vez de aparecer y fallar al pulsarlo.
     */
    @GetMapping("/config")
    public ApiResponse<ConfigGoogleResponse> config() {
        return ApiResponse.ok(new ConfigGoogleResponse(true, props.clientId()));
    }

    /**
     * Cambia el {@code id_token} de Google por una sesión de la aplicación.
     *
     * Devuelve exactamente lo mismo que {@code /api/auth/login}, para que al front le dé igual
     * por dónde haya entrado el usuario.
     */
    @PostMapping
    public ApiResponse<LoginResponse> entrar(@Valid @RequestBody LoginGoogleRequest peticion) {
        return ApiResponse.ok(servicio.entrar(peticion.credential()), "Login correcto");
    }

    /**
     * Si la cuenta de quien pregunta entró por Google.
     *
     * Lo consulta la página de cuenta para no ofrecer «cambiar contraseña» a alguien que no
     * tiene una: el formulario le pediría la actual y no habría forma de rellenarlo.
     *
     * Exige sesión aunque cuelgue de una ruta pública, porque {@code @CurrentUser} lanza 401 si
     * no hay token. Cada cual pregunta por sí mismo y por nadie más.
     */
    @GetMapping("/vinculada")
    public ApiResponse<VinculoGoogleResponse> vinculada(@CurrentUser AuthUser usuario) {
        return ApiResponse.ok(new VinculoGoogleResponse(servicio.estaVinculada(usuario.id())));
    }
}
