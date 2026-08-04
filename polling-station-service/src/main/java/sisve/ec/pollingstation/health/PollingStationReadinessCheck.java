package sisve.ec.pollingstation.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import sisve.ec.pollingstation.repository.ElectoralRollRepository;

@Readiness
@ApplicationScoped
public class PollingStationReadinessCheck implements HealthCheck {

    @Inject
    ElectoralRollRepository electoralRollRepository;

    @Override
    public HealthCheckResponse call() {
        try {
            electoralRollRepository.count();
            return HealthCheckResponse
                    .named("polling-station-service-ready")
                    .up()
                    .withData("service", "polling-station-service")
                    .build();
        } catch (Exception exception) {
            return HealthCheckResponse
                    .named("polling-station-service-ready")
                    .down()
                    .withData("service", "polling-station-service")
                    .build();
        }
    }
}
