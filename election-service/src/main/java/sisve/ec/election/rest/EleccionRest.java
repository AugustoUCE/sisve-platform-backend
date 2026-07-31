package sisve.ec.election.rest;

import sisve.ec.election.dto.CargoRequest;
import sisve.ec.election.dto.CargoResponse;
import sisve.ec.election.dto.EstadoParticipacionResponse;
import sisve.ec.election.dto.EleccionRequest;
import sisve.ec.election.dto.EleccionResponse;
import sisve.ec.election.dto.PadronCargaRequest;
import sisve.ec.election.service.ElectionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/elecciones")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EleccionRest {

    @Inject
    ElectionService electionService;

    @POST
    public Response crearEleccion(@Valid EleccionRequest request) {
        EleccionResponse response = electionService.crearEleccion(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/activas")
    public List<EleccionResponse> listarActivas() {
        return electionService.listarActivas();
    }

    @GET
    @Path("/{id}")
    public EleccionResponse obtenerPorId(@PathParam("id") Long idEleccion) {
        return electionService.obtenerEleccion(idEleccion);
    }

    @GET
    @Path("/{id}/cargos")
    public List<CargoResponse> listarCargos(@PathParam("id") Long idEleccion) {
        return electionService.listarCargosPorEleccion(idEleccion);
    }

    @GET
    @Path("/{id}/candidatos")
    public List<sisve.ec.election.dto.CandidatoResponse> listarCandidatosPorEleccion(@PathParam("id") Long idEleccion) {
        return electionService.listarCandidatosPorEleccion(idEleccion);
    }

    @POST
    @Path("/{id}/cargos")
    public Response crearCargo(@PathParam("id") Long idEleccion, @Valid CargoRequest request) {
        CargoResponse response = electionService.crearCargo(idEleccion, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/{id}/padron")
    public Response cargarPadron(@PathParam("id") Long idEleccion, @Valid PadronCargaRequest request) {
        electionService.cargarPadron(idEleccion, request);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/habilitado/{idVotante}")
    public Map<String, Boolean> verificarHabilitado(@PathParam("id") Long idEleccion, @PathParam("idVotante") Long idVotante) {
        return Map.of("habilitado", electionService.verificarHabilitado(idEleccion, idVotante));
    }

    @GET
    @Path("/{id}/votantes/{idVotante}/estado")
    public EstadoParticipacionResponse estadoParticipacion(@PathParam("id") Long idEleccion, @PathParam("idVotante") Long idVotante) {
        return electionService.obtenerEstadoParticipacion(idEleccion, idVotante);
    }

    @PUT
    @Path("/{id}/marcar-votado/{idVotante}")
    public Response marcarVotado(@PathParam("id") Long idEleccion, @PathParam("idVotante") Long idVotante) {
        electionService.marcarVotado(idEleccion, idVotante);
        return Response.noContent().build();
    }
}