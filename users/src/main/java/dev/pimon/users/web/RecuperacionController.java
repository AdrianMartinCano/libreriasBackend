package dev.pimon.users.web;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.users.dto.RecuperacionRequests;
import dev.pimon.users.service.RecuperacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recuperar la contrasena olvidada.
 *
 * <h2>Cuelga de /api/auth, que ya es publico</h2>
 *
 * Tiene que serlo: quien lo usa no puede entrar, que es justo el problema que viene a
 * resolver. Al colgar de {@code /api/auth/**}, que ya esta en {@code public-paths} por
 * defecto, no hace falta que cada proyecto se acuerde de abrirlo — y olvidarse habria
 * dejado un 401 en el unico sitio donde no se puede pedir una sesion.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RecuperacionController {

    /**
     * La misma frase exista el correo o no.
     *
     * Es el centro de todo: si aqui se distinguiera, esta pantalla seria un buscador de
     * clientes —se prueban direcciones y se apunta cuales contestan distinto—. Por eso el
     * servicio tampoco lanza cuando no encuentra la cuenta.
     */
    private static final String MISMA_RESPUESTA_SIEMPRE =
            "Si ese correo tiene una cuenta, te hemos mandado un enlace para cambiar la "
            + "contraseña. Revisa también la carpeta de spam.";

    private final RecuperacionService recuperacion;

    @PostMapping("/recuperar")
    public ApiResponse<Void> solicitar(
            @Valid @RequestBody RecuperacionRequests.Solicitar req) {
        recuperacion.solicitar(req.email());
        return ApiResponse.noContent(MISMA_RESPUESTA_SIEMPRE);
    }

    @PostMapping("/restablecer")
    public ApiResponse<Void> restablecer(
            @Valid @RequestBody RecuperacionRequests.Restablecer req) {
        recuperacion.restablecer(req.token(), req.password());
        return ApiResponse.noContent("Listo. Ya puedes entrar con tu contraseña nueva.");
    }
}
