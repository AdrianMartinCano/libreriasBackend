package dev.pimon.users.dto;

import dev.pimon.users.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Alta de un usuario por un administrador (p. ej. el jefe creando la cuenta de un trabajador). A
 * diferencia de {@link RegisterRequest} (auto-registro público, siempre ROLE_USER), permite fijar los
 * roles —típicamente {@code ROLE_STAFF}— en el momento del alta.
 */
public record AdminCreateUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,
        Set<Role> roles,
        Boolean active
) {}
