package sisve.ec.pollingstation.lifecycle;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import sisve.ec.pollingstation.repository.PollingStationRepository;

@ApplicationScoped
public class PollingStationStartup {

    private static final Logger LOGGER = Logger.getLogger(PollingStationStartup.class);

    @Inject
    PollingStationRepository pollingStationRepository;

    void onStart(@Observes StartupEvent event) {
        LOGGER.infof("polling-station-service iniciado. Mesas registradas: %d", pollingStationRepository.count());
    }
}
