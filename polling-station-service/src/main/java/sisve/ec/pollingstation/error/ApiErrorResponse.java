package sisve.ec.pollingstation.error;

public record ApiErrorResponse(
        String codigo,
        String mensaje,
        String detalle,
        Integer status
) {
}
