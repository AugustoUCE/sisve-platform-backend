package sisve.ec.auth.dto;

public record EventoAuditoriaDTO(
        String tipoEvento,
        String descripcion,
        String servicioOrigen
) {
}