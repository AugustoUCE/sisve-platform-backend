package sisve.ec.election.dto;

public record EstadoParticipacionResponse(
        Long idEleccion,
        Long idVotante,
        Boolean habilitado,
        Boolean haVotado
) {
}
