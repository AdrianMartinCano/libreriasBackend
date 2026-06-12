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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth;
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            // Captura BadCredentialsException, InternalAuthenticationServiceException,
            // UsernameNotFoundException, DisabledException, LockedException...
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
}
