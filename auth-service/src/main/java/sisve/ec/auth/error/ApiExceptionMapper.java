package sisve.ec.auth.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            Response response = webApplicationException.getResponse();
            String detalle = detalleDesde(response.getEntity(), response.getStatusInfo().getReasonPhrase());
            return Response.status(response.getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiErrorResponse(codigoDesde(response.getStatus(), detalle),
                            mensajeDesde(response.getStatus(), detalle),
                            detalle,
                            response.getStatus()))
                    .build();
        }

        String detalle = detalleDesde(exception.getMessage(), "Error interno del servidor");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiErrorResponse("ERROR_INTERNO", "Error interno del servidor", detalle, 500))
                .build();
    }

    private String detalleDesde(Object entity, String fallback) {
        if (entity == null) {
            return fallback;
        }
        String value = entity.toString().trim();
        return value.isBlank() ? fallback : value;
    }

    private String mensajeDesde(int status, String detalle) {
        return switch (status) {
            case 401 -> detalle.contains("credencial") ? "Credenciales inválidas" : "Token inválido";
            case 403 -> detalle.contains("inactivo") ? "Votante no habilitado" : detalle;
            case 404 -> detalle;
            case 409 -> "Conflicto de negocio";
            default -> detalle;
        };
    }

    private String codigoDesde(int status, String detalle) {
        String normalized = detalle.toLowerCase();
        if (status == 401 && normalized.contains("credencial")) {
            return "CREDENCIALES_INVALIDAS";
        }
        if (status == 401) {
            return "TOKEN_INVALIDO";
        }
        if (status == 403 && normalized.contains("inactivo")) {
            return "VOTANTE_NO_HABILITADO";
        }
        if (status == 403 && normalized.contains("no esta habilitado")) {
            return "VOTANTE_NO_HABILITADO";
        }
        if (status == 403 && normalized.contains("activa")) {
            return "ELECCION_INACTIVA";
        }
        if (status == 404 && normalized.contains("cargo")) {
            return "CARGO_INVALIDO";
        }
        if (status == 404 && normalized.contains("candidato")) {
            return "CANDIDATO_INVALIDO";
        }
        if (status == 409 && normalized.contains("voto")) {
            return "VOTO_DUPLICADO";
        }
        if (status == 404) {
            return "RECURSO_NO_ENCONTRADO";
        }
        if (status == 409) {
            return "CONFLICTO";
        }
        return status >= 500 ? "ERROR_INTERNO" : "ERROR_NEGOCIO";
    }
}
