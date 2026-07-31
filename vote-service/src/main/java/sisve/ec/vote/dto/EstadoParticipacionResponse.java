package sisve.ec.vote.dto;

public record EstadoParticipacionResponse(
        Long idEleccion,
        Long idVotante,
        Boolean habilitado,
        Boolean haVotado
) {
}
