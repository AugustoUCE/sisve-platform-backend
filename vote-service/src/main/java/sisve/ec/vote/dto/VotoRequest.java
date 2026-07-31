package sisve.ec.vote.dto;

import jakarta.validation.constraints.NotNull;

public record VotoRequest(
        @NotNull Long idEleccion,
        @NotNull Long idCargo,
        @NotNull Long idCandidato,
        @NotNull Long idVotante
) {
}