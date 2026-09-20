package sisve.ec.vote.dto;

import java.time.LocalDateTime;

public record VotoResponse(
        String mensaje,
        Long idEleccion,
        String tipoVoto,
        String hashActual,
        LocalDateTime fechaRegistro
) {
}