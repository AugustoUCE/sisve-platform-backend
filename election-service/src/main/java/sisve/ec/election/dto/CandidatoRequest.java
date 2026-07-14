package sisve.ec.election.dto;

import jakarta.validation.constraints.NotBlank;

public record CandidatoRequest(
        @NotBlank String nombres,
        @NotBlank String apellidos,
        String lista
) {
}