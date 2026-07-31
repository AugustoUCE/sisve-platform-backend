package sisve.ec.election.error;

public record ApiErrorResponse(
        String codigo,
        String mensaje,
        String detalle,
        Integer status
) {
}
