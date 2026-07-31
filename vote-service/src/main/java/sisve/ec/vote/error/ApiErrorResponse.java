package sisve.ec.vote.error;

public record ApiErrorResponse(
        String codigo,
        String mensaje,
        String detalle,
        Integer status
) {
}
