package sisve.ec.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String cedula,
        @NotBlank String correoInstitucional
) {
}