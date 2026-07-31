package sisve.ec.vote.dto;

public record ValidateResponse(
        Boolean valido,
        Long idVotante,
        String cedula,
        String correoInstitucional,
        String nombres,
        String apellidos
) {
}
