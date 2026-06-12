package dev.pimon.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.security.config.SecurityProperties;
import dev.pimon.security.model.AuthUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Genera y valida tokens JWT.
 *
 * El token incluye:
 *   - sub:   id del usuario (username del UserDetails)
 *   - email: email del usuario (si se pasa explícitamente)
 *   - roles: lista de roles
 *   - iat:   fecha de emisión
 *   - exp:   fecha de expiración
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final SecurityProperties props;

    /** Genera un token a partir del UserDetails de Spring Security */
    public String generateToken(UserDetails user) {
        return generateToken(user.getUsername(), user.getUsername(),
                user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());
    }

    /** Genera un token con id, email y roles explícitos */
    public String generateToken(String id, String email, List<String> roles) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + props.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(id)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    /** Valida el token — devuelve true si es válido y no ha expirado */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado");
        } catch (JwtException e) {
            log.warn("JWT inválido: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT vacío o nulo");
        }
        return false;
    }

    /** Extrae el AuthUser del token (sin validar — usar solo tras validateToken) */
    @SuppressWarnings("unchecked")
    public AuthUser extractUser(String token) {
        Claims claims = parseClaims(token);
        List<String> roles = claims.get("roles", List.class);
        return new AuthUser(
                claims.getSubject(),
                claims.get("email", String.class),
                roles != null ? roles : List.of()
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = props.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
