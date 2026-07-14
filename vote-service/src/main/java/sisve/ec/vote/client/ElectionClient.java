package sisve.ec.vote.client;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import sisve.ec.vote.dto.EleccionResponse;

import java.util.Map;

@RegisterRestClient(configKey = "election-service")
@Path("/elecciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ElectionClient {

    @GET
    @Path("/{idEleccion}")
    EleccionResponse getEleccion(@PathParam("idEleccion") Long idEleccion);

    @GET
    @Path("/{idEleccion}/habilitado/{idVotante}")
    Map<String, Boolean> verificarHabilitado(@PathParam("idEleccion") Long idEleccion,
                                             @PathParam("idVotante") Long idVotante);

    @PUT
    @Path("/{idEleccion}/marcar-votado/{idVotante}")
    void marcarVotado(@PathParam("idEleccion") Long idEleccion,
                      @PathParam("idVotante") Long idVotante);
}