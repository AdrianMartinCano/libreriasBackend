package dev.pimon.forum.service;

import dev.pimon.security.model.AuthUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resuelve nicks a mostrar. Centraliza el "nick en vivo": guardamos el nombre al
 * escribir, pero al servir resolvemos el actual con {@link ForumAuthorNameResolver}
 * (si la app lo provee), de modo que cambiar el nick se refleja en todo el foro.
 */
@Component
public class ForumNames {

    private final Optional<ForumAuthorNameResolver> resolver;

    public ForumNames(Optional<ForumAuthorNameResolver> resolver) {
        this.resolver = resolver;
    }

    /** Nick actual de un usuario por id, o null si no se puede resolver. */
    public String live(String userId) {
        return resolver.map(r -> r.resolve(userId))
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse(null);
    }

    /** Nick a guardar al escribir: el del perfil o, si no, la parte local del email. */
    public String forWriting(AuthUser user) {
        String resolved = live(user.id());
        if (resolved != null) return resolved;
        String email = user.email();
        if (email == null || email.isBlank()) return "Usuario";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    /** Nick a mostrar: el del perfil (en vivo) o, si no, el guardado. */
    public String forDisplay(String userId, String stored) {
        String live = live(userId);
        return live != null ? live : stored;
    }
}
