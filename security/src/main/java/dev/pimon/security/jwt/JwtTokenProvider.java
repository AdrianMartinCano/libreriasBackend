package dev.pimon.security.jwt;

import dev.pimon.security.config.SecurityProperties;
import dev.pimon.security.model.AuthUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Genera y valida tokens JWT (HS256).
 *
 * <p>El token incluye:</p>
 * <ul>
 *   <li>{@code sub}: id del usuario</li>
 *   <li>{@code email}: email del usuario</li>
 *   <li>{@code roles}: lista de roles</li>
 *   <li>{@code iat}/{@code exp}: emisión y expiración</li>
 * </ul>
 *
 * <p><b>Eficiencia:</b> la {@link SecretKey} se deriva <b>una sola vez</b> al arrancar
 * (no en cada operación) y la validación + extracción del usuario se hacen con un
 * <b>único parseo</b> por petición (ver {@link #parseToken(String)}).</p>
 *
 * <p><b>Seguridad:</b> al arrancar se valida que el secreto tenga al menos 256 bits
 * (requisito de HS256) y se avisa si se está usando el secreto por defecto.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /** HS256 exige una clave de al menos 256 bits = 32 bytes. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecurityProperties props;

    /** Clave derivada una sola vez al arrancar (no se reconstruye en cada operación). */
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = props.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "pimon.security.jwt-secret es demasiado corto: " + keyBytes.length +
                    " bytes. HS256 requiere al menos " + MIN_SECRET_BYTES + " (256 bits).");
        }
        if (SecurityProperties.DEFAULT_JWT_SECRET.equals(props.getJwtSecret())) {
            log.warn("⚠️  Usando el JWT secret POR DEFECTO. Es público: cualquiera puede falsificar " +
                     "tokens. Define 'pimon.security.jwt-secret' antes de ir a producción.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Genera un token a partir del UserDetails de Spring Security. */
    public String generateToken(UserDetails user) {
        return generateToken(user.getUsername(), user.getUsername(),
                user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());
    }

    /** Genera un token con id, email y roles explícitos. */
    public String generateToken(String id, String email, List<String> roles) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + props.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(id)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Valida el token y extrae el usuario en un <b>único parseo</b> (verifica firma + expiración).
     *
     * @return el {@link AuthUser} si el token es válido y no ha expirado; {@code null} en caso contrario.
     */
    public AuthUser parseToken(String token) {
        try {
            return toAuthUser(parseClaims(token));
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado");
        } catch (JwtException e) {
            log.warn("JWT inválido: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT vacío o nulo");
        }
        return null;
    }

    /** Valida el token — devuelve true si es válido y no ha expirado. */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /** Extrae el AuthUser del token (lanza si no es válido — preferir {@link #parseToken}). */
    public AuthUser extractUser(String token) {
        return toAuthUser(parseClaims(token));
    }

    @SuppressWarnings("unchecked")
    private AuthUser toAuthUser(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        return new AuthUser(
                claims.getSubject(),
                claims.get("email", String.class),
                roles != null ? roles : List.of()
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
