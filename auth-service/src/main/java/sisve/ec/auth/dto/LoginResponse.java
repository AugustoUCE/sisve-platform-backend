package sisve.ec.auth.dto;

public record LoginResponse(
        String token,
        Long idVotante,
        String nombres,
        String apellidos
) {
}