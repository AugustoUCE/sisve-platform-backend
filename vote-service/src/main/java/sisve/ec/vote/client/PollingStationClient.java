package sisve.ec.vote.client;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
    @Path("/election/{idElection}/voters/{idVoter}/eligibility")
    Map<String, Object> validarElegibilidad(@PathParam("idElection") Long idElection,
                                            @PathParam("idVoter") Long idVoter);

    @POST
    @Path("/election/{idElection}/voters/{idVoter}/mark-voted")
    void marcarVotado(@PathParam("idElection") Long idElection,
                      @PathParam("idVoter") Long idVoter);
}
