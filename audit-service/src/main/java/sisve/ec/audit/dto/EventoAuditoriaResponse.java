package sisve.ec.audit.dto;

import java.time.LocalDateTime;

public record EventoAuditoriaResponse(
        Long idAuditoria,
        String tipoEvento,
        String descripcion,
        LocalDateTime fechaEvento,
        String ipOrigen,
        String servicioOrigen) {
}