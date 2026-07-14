package sisve.ec.vote.dto;

public record EventoAuditoriaDTO(
        String tipoEvento,
        String descripcion,
        String servicioOrigen
) {
}