package sisve.ec.audit.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sisve.ec.audit.dto.EventoAuditoriaRequest;
import sisve.ec.audit.service.AuditService;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Path("/auditoria")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditRest {

    @Inject
    AuditService auditService;

    @POST
    @Path("/eventos")
    public Response registrarEvento(@Valid EventoAuditoriaRequest request) {
        return Response.status(Response.Status.CREATED).entity(auditService.registrarEvento(request)).build();
    }

    @GET
    @Path("/eventos")
    public Response listarPorServicio(@QueryParam("servicio") String servicio, @QueryParam("desde") String desde) {
        if (servicio == null || servicio.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El parámetro 'servicio' es obligatorio.")
                    .build();
        }

        LocalDateTime desdeDate = null;
        if (desde != null && !desde.isBlank()) {
            try {
                desdeDate = LocalDateTime.parse(desde);
            } catch (DateTimeParseException exception) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El parámetro 'desde' debe usar el formato ISO-8601 LocalDateTime.")
                        .build();
            }
        }

        return Response.ok(auditService.listarPorServicio(servicio, desdeDate)).build();
    }

    @GET
    @Path("/eventos/tipo/{tipoEvento}")
    public Response listarPorTipoEvento(@PathParam("tipoEvento") String tipoEvento) {
        return Response.ok(auditService.listarPorTipoEvento(tipoEvento)).build();
    }
}