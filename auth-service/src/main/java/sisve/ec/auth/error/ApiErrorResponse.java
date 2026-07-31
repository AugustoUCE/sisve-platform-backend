package sisve.ec.auth.error;

public record ApiErrorResponse(
        String codigo,
        String mensaje,
        String detalle,
        Integer status
) {
}
