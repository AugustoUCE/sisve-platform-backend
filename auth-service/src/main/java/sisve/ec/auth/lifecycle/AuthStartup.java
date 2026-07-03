package sisve.ec.auth.lifecycle;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthStartup {

    private static final Logger LOGGER = Logger.getLogger(AuthStartup.class);

    void onStart(@Observes StartupEvent event) {
        LOGGER.info("auth-service iniciado");
    }

    void onStop(@Observes ShutdownEvent event) {
        LOGGER.info("auth-service detenido");
    }
}