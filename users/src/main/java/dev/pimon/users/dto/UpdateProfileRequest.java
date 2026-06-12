package dev.pimon.users.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name
) {}
