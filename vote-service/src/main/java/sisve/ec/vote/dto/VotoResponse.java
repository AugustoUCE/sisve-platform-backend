package sisve.ec.vote.dto;

import java.time.LocalDateTime;

public record VotoResponse(
        String mensaje,
        Long idEleccion,
        Long idVotante,
        String hashActual,
        LocalDateTime fechaRegistro
) {
}