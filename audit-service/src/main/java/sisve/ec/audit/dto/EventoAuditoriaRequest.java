package sisve.ec.audit.dto;

import jakarta.validation.constraints.NotBlank;

public record EventoAuditoriaRequest(
        @NotBlank String tipoEvento,
        String descripcion,
        String ipOrigen,
        @NotBlank String servicioOrigen) {
}