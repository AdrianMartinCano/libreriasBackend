package dev.pimon.security.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.config.SecurityProperties;
import dev.pimon.security.dto.LoginRequest;
import dev.pimon.security.dto.LoginResponse;
import dev.pimon.security.jwt.JwtTokenProvider;
import dev.pimon.security.model.AuthUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de autenticación.
 * Se registra automáticamente al incluir libui-security.
 *
 * POST /api/auth/login  — devuelve JWT
 * GET  /api/auth/me     — devuelve el usuario autenticado
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider      jwtProvider;
    private final SecurityProperties    props;
    private final UserDetailsService    usuarios;
    private final PasswordEncoder       encoder;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth;
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (DisabledException e) {
            throw deCuentaDesactivada(request);

        } catch (AuthenticationException e) {
            // Captura BadCredentialsException, InternalAuthenticationServiceException,
            // UsernameNotFoundException, LockedException...
            throw AppException.unauthorized("Email o contraseña incorrectos");
        }

        UserDetails user  = (UserDetails) auth.getPrincipal();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = jwtProvider.generateToken(
                user.getUsername(), request.email(), roles
        );

        return ApiResponse.ok(new LoginResponse(
                token,
                props.getAccessTokenExpiration(),
                user.getUsername(),
                request.email(),
                roles
        ), "Login correcto");
    }

    @GetMapping("/me")
    public ApiResponse<AuthUser> me(@CurrentUser AuthUser user) {
        return ApiResponse.ok(user);
    }

    /**
     * Qué contestar cuando la cuenta existe pero está dada de baja.
     *
     * <h2>Se dice, pero solo a quien acierta la contraseña</h2>
     *
     * Spring comprueba si la cuenta está activa <b>antes</b> de mirar la contraseña, así que
     * contestar directamente «esta cuenta está dada de baja» se lo diría también a quien
     * solo ha acertado el correo. Eso convierte el login en una herramienta para averiguar
     * <b>quién tiene cuenta aquí</b>, probando direcciones una por una.
     *
     * Por eso se comprueba la contraseña a mano antes de decir nada. A quien la acierta no
     * se le cuenta nada que no sepa ya — son sus credenciales— y se le ahorra el peor de
     * los mensajes: «email o contraseña incorrectos» cuando los dos son correctos, que le
     * manda a probar una y otra vez algo que nunca va a funcionar.
     *
     * <h2>Por qué no basta con mirar el estado</h2>
     *
     * Porque saber que una cuenta existe y está desactivada <b>es información</b>. Quien
     * la pide sin conocer la contraseña no tiene derecho a ella.
     */
    private AppException deCuentaDesactivada(LoginRequest request) {
        try {
            UserDetails usuario = usuarios.loadUserByUsername(request.email());
            if (encoder.matches(request.password(), usuario.getPassword())) {
                return AppException.unauthorized(
                        "Esta cuenta está dada de baja. Escríbenos si crees que es un error.");
            }
        } catch (Exception ignorado) {
            // Si no se puede comprobar, se cae al mensaje genérico: ante la duda, no contar.
        }
        return AppException.unauthorized("Email o contraseña incorrectos");
    }
}
