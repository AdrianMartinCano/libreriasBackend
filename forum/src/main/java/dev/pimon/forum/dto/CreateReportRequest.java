package dev.pimon.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Denunciar un mensaje. El motivo es obligatorio (validado también en servidor). */
public record CreateReportRequest(
        @NotBlank(message = "El motivo de la denuncia es obligatorio")
        @Size(max = 1000, message = "El motivo es demasiado largo")
        String reason
) {}
