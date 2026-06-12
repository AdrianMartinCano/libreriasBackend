package dev.pimon.security.dto;

import java.util.List;

public record LoginResponse(
        String       token,
        long         expiresIn,    // ms — para que el frontend sepa cuándo expira
        String       id,
        String       email,
        List<String> roles
) {}
