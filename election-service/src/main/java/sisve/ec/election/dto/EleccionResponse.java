package sisve.ec.election.dto;

import java.time.LocalDateTime;

public record EleccionResponse(
        Long idEleccion,
        String nombre,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String estado
) {
}