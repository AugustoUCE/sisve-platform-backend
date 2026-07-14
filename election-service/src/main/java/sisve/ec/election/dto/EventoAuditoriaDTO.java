package sisve.ec.election.dto;

public record EventoAuditoriaDTO(
        String tipoEvento,
        String descripcion,
        String servicioOrigen
) {
}