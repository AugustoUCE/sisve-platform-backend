package sisve.ec.pollingstation.dto;

public record EventoAuditoriaDTO(
        String tipoEvento,
        String descripcion,
        String ipOrigen,
        String servicioOrigen
) {
}
