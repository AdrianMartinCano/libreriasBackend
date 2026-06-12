package dev.pimon.users.dto;

import jakarta.validation.constraints.NotBlank;
import dev.pimon.users.entity.Role;

import java.util.Set;

public record AdminUpdateUserRequest(
        @NotBlank String name,
        Set<Role>  roles,
        Boolean    active
) {}
