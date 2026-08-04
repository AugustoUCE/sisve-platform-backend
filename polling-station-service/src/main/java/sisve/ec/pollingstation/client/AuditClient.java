package sisve.ec.pollingstation.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import sisve.ec.pollingstation.dto.EventoAuditoriaDTO;

@RegisterRestClient(configKey = "audit-service")
@Path("/auditoria/eventos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuditClient {

    @POST
    void registrarEvento(EventoAuditoriaDTO evento);
}
