package sisve.ec.election.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EleccionRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull LocalDateTime fechaInicio,
        @NotNull LocalDateTime fechaFin
) {
}