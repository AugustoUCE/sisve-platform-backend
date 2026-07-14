package sisve.ec.election.lifecycle;

import sisve.ec.election.repository.EleccionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ElectionStartup {

    private static final Logger LOG = Logger.getLogger(ElectionStartup.class);

    @Inject
    EleccionRepository eleccionRepository;

    void onStart(@Observes StartupEvent event) {
        LOG.info("election-service iniciado");
        LOG.infof("Elecciones activas en BD: %d", eleccionRepository.findActivas().size());
    }

    void onStop(@Observes ShutdownEvent event) {
        LOG.info("election-service detenido");
    }
}