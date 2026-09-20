package sisve.ec.vote.client;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "polling-station-service")
@Path("/polling-stations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PollingStationClient {

    @GET
    @Path("/me/eligibility")
    Map<String, Object> validarElegibilidad(@HeaderParam("Authorization") String authHeader,
                                            @jakarta.ws.rs.QueryParam("electionId") Long idElection);

    @POST
    @Path("/me/election/{idElection}/mark-voted")
    void marcarVotado(@PathParam("idElection") Long idElection,
                      @HeaderParam("Authorization") String authHeader);

    @POST
    @Path("/me/election/{idElection}/reserve-vote")
    void reservarVoto(@PathParam("idElection") Long idElection,
                      @HeaderParam("Authorization") String authHeader);

    @POST
    @Path("/me/election/{idElection}/complete-vote")
    void completarVoto(@PathParam("idElection") Long idElection,
                       @HeaderParam("Authorization") String authHeader);

    @POST
    @Path("/me/election/{idElection}/release-vote")
    void liberarReserva(@PathParam("idElection") Long idElection,
                        @HeaderParam("Authorization") String authHeader);
}
