package sisve.ec.vote.dto;

import jakarta.validation.constraints.NotNull;
import sisve.ec.vote.db.TipoVoto;

public record VotoRequest(
        @NotNull Long idEleccion,
        @NotNull Long idCargo,
        Long idCandidato,
        @NotNull TipoVoto tipoVoto
) {
}