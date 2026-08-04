package sisve.ec.pollingstation.error;

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
            case 404 -> detalle;
            case 409 -> detalle;
            case 403 -> detalle;
            default -> detalle;
        };
    }

    private String codigoDesde(int status, String detalle) {
        String normalized = detalle.toLowerCase();
        if (status == 404) {
            return "RECURSO_NO_ENCONTRADO";
        }
        if (status == 409 && normalized.contains("mesa")) {
            return "ESTADO_MESA_INVALIDO";
        }
        if (status == 409 && normalized.contains("votante")) {
            return "ESTADO_VOTANTE_INVALIDO";
        }
        if (status == 403 && normalized.contains("habilitado")) {
            return "VOTANTE_NO_HABILITADO";
        }
        if (status == 500) {
            return "ERROR_INTERNO";
        }
        return "ERROR_NEGOCIO";
    }
}
