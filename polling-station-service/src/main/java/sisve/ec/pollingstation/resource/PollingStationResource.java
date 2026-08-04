package sisve.ec.pollingstation.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sisve.ec.pollingstation.dto.BlockVoterRequest;
import sisve.ec.pollingstation.dto.CloseStationRequest;
import sisve.ec.pollingstation.dto.EnableVoterRequest;
import sisve.ec.pollingstation.dto.ElectoralRollResponse;
import sisve.ec.pollingstation.dto.OpenStationRequest;
import sisve.ec.pollingstation.dto.PollingStationEligibilityResponse;
import sisve.ec.pollingstation.dto.PollingStationMemberResponse;
import sisve.ec.pollingstation.dto.PollingStationResponse;
import sisve.ec.pollingstation.dto.PollingStationSummaryResponse;
import sisve.ec.pollingstation.service.PollingStationService;

import java.util.List;

@Path("/polling-stations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PollingStationResource {

    @Inject
    PollingStationService pollingStationService;

    @GET
    @Path("/election/{idElection}")
    public List<PollingStationResponse> listarPorEleccion(@PathParam("idElection") Long idElection) {
        return pollingStationService.listarPorEleccion(idElection);
    }

    @GET
    @Path("/{idPollingStation}")
    public PollingStationResponse obtenerPorId(@PathParam("idPollingStation") Long idPollingStation) {
        return pollingStationService.obtenerPorId(idPollingStation);
    }

    @GET
    @Path("/{idPollingStation}/electoral-roll")
    public List<ElectoralRollResponse> obtenerPadron(@PathParam("idPollingStation") Long idPollingStation) {
        return pollingStationService.obtenerPadron(idPollingStation);
    }

    @GET
    @Path("/{idPollingStation}/members")
    public List<PollingStationMemberResponse> listarMiembros(@PathParam("idPollingStation") Long idPollingStation) {
        return pollingStationService.listarMiembros(idPollingStation);
    }

    @GET
    @Path("/{idPollingStation}/voters/search")
    public ElectoralRollResponse buscarVotantePorCedula(@PathParam("idPollingStation") Long idPollingStation,
                                                        @QueryParam("cedula") String cedula) {
        return pollingStationService.buscarVotantePorCedula(idPollingStation, cedula);
    }

    @POST
    @Path("/{idPollingStation}/voters/{idVoter}/enable")
    public Response habilitarVotante(@PathParam("idPollingStation") Long idPollingStation,
                                     @PathParam("idVoter") Long idVoter,
                                     @Valid EnableVoterRequest request) {
        return Response.ok(pollingStationService.habilitarVotante(idPollingStation, idVoter, request)).build();
    }

    @POST
    @Path("/{idPollingStation}/voters/{idVoter}/block")
    public Response bloquearVotante(@PathParam("idPollingStation") Long idPollingStation,
                                    @PathParam("idVoter") Long idVoter,
                                    @Valid BlockVoterRequest request) {
        return Response.ok(pollingStationService.bloquearVotante(idPollingStation, idVoter, request)).build();
    }

    @POST
    @Path("/election/{idElection}/voters/{idVoter}/mark-voted")
    public Response marcarVotado(@PathParam("idElection") Long idElection,
                                 @PathParam("idVoter") Long idVoter) {
        return Response.ok(pollingStationService.marcarVotanteComoVotado(idElection, idVoter)).build();
    }

    @GET
    @Path("/election/{idElection}/voters/{idVoter}/eligibility")
    public PollingStationEligibilityResponse validarElegibilidad(@PathParam("idElection") Long idElection,
                                                                 @PathParam("idVoter") Long idVoter) {
        return pollingStationService.validarElegibilidad(idElection, idVoter);
    }

    @POST
    @Path("/{idPollingStation}/close")
    public Response cerrarMesa(@PathParam("idPollingStation") Long idPollingStation,
                               @Valid CloseStationRequest request) {
        return Response.ok(pollingStationService.cerrarMesa(idPollingStation, request)).build();
    }

    @POST
    @Path("/{idPollingStation}/open")
    public Response abrirMesa(@PathParam("idPollingStation") Long idPollingStation,
                              @Valid OpenStationRequest request) {
        return Response.ok(pollingStationService.abrirMesa(idPollingStation, request)).build();
    }

    @GET
    @Path("/{idPollingStation}/summary")
    public PollingStationSummaryResponse resumen(@PathParam("idPollingStation") Long idPollingStation) {
        return pollingStationService.resumen(idPollingStation);
    }
}
