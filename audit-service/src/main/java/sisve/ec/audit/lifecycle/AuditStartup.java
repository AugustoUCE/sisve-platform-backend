package sisve.ec.audit.lifecycle;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuditStartup {

    private static final Logger LOGGER = Logger.getLogger(AuditStartup.class);

    private final MeterRegistry meterRegistry;

    public AuditStartup(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    void onStart(@Observes StartupEvent event) {
        meterRegistry.counter("audit_service_startup_total").increment();
        LOGGER.info("audit-service iniciado");
    }

    void onStop(@Observes ShutdownEvent event) {
        meterRegistry.counter("audit_service_shutdown_total").increment();
        LOGGER.info("audit-service detenido");
    }
}