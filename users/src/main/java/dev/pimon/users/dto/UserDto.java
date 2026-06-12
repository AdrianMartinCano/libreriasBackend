package dev.pimon.users.dto;

import java.time.LocalDateTime;
import java.util.Set;

/** Representación pública del usuario — nunca expone la contraseña. */
public record UserDto(
        String          id,
        String          email,
        String          name,
        Set<String>     roles,
        boolean         active,
        LocalDateTime   createdAt
) {}
