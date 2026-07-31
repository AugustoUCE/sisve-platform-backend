package sisve.ec.auth.rest;

import sisve.ec.auth.dto.LoginRequest;
import sisve.ec.auth.dto.LoginResponse;
import sisve.ec.auth.dto.ValidateResponse;
import sisve.ec.auth.service.AuthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthRest {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public LoginResponse login(LoginRequest request) {
        return authService.login(request);
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    public Response logout(@HeaderParam("Authorization") String authorization) {
        authService.logout(extraerToken(authorization));
        return Response.noContent().build();
    }

    @GET
    @Path("/validate")
    public ValidateResponse validate(@HeaderParam("Authorization") String authorization) {
        return authService.validarToken(extraerToken(authorization));
    }

    private String extraerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}