package sisve.ec.auth.dto;

public record ValidateResponse(
        Boolean valido,
        Long idVotante,
        String cedula,
        String correoInstitucional,
        String nombres,
        String apellidos,
        Boolean voto
) {
}
