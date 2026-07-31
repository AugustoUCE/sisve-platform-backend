package sisve.ec.vote.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import sisve.ec.vote.dto.CandidatoResponse;

import java.util.List;

@RegisterRestClient(configKey = "election-service")
@Path("/cargos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CargoClient {

    @GET
    @Path("/{idCargo}/candidatos")
    List<CandidatoResponse> getCandidatosPorCargo(@PathParam("idCargo") Long idCargo);
}
