package sisve.ec.vote.rest;


import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sisve.ec.vote.dto.VotoRequest;
import sisve.ec.vote.service.VoteService;

import java.util.Map;

@Path("/votos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VoteRest {

    @Inject
    VoteService voteService;

    @POST
    public Response emitirVoto(@Valid VotoRequest request, @HeaderParam("Authorization") String authHeader) {
        return Response.ok(voteService.emitirVoto(request, authHeader)).build();
    }

    @GET
    @Path("/eleccion/{idEleccion}/verificar-integridad")
    public Response verificarIntegridad(@PathParam("idEleccion") Long idEleccion) {
        return Response.ok(Map.of("integridadValida", voteService.verificarIntegridad(idEleccion))).build();
    }
}