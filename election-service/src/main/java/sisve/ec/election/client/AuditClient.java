package sisve.ec.election.client;

import sisve.ec.election.dto.EventoAuditoriaDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "audit-service")
@Path("/auditoria/eventos")
@Consumes(MediaType.APPLICATION_JSON)
public interface AuditClient {

    @POST
    void registrarEvento(EventoAuditoriaDTO evento);
}