package sisve.ec.audit.error;

public record ApiErrorResponse(
        String codigo,
        String mensaje,
        String detalle,
        Integer status
) {
}
