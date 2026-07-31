package sisve.ec.vote.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import sisve.ec.vote.dto.ValidateResponse;

@RegisterRestClient(configKey = "auth-service")
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthClient {

    @GET
    @Path("/validate")
    ValidateResponse validarToken(@HeaderParam("Authorization") String authHeader);

    @POST
    @Path("/logout")
    void logout(@HeaderParam("Authorization") String authHeader);
}