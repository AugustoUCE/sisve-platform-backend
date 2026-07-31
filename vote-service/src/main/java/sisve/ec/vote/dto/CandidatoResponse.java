package sisve.ec.vote.dto;

public record CandidatoResponse(
        Long idCandidato,
        Long idCargo,
        String nombres,
        String apellidos,
        String lista,
        Boolean estado
) {
}
