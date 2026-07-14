package sisve.ec.election.rest;

import sisve.ec.election.dto.CandidatoRequest;
import sisve.ec.election.dto.CandidatoResponse;
import sisve.ec.election.service.ElectionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/cargos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CargoRest {

    @Inject
    ElectionService electionService;

    @POST
    @Path("/{id}/candidatos")
    public Response crearCandidato(@PathParam("id") Long idCargo, @Valid CandidatoRequest request) {
        CandidatoResponse response = electionService.crearCandidato(idCargo, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{id}/candidatos")
    public List<CandidatoResponse> listarPorCargo(@PathParam("id") Long idCargo) {
        return electionService.listarCandidatosPorCargo(idCargo);
    }
}