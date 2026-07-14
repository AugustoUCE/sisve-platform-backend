package sisve.ec.election.health;

import sisve.ec.election.repository.EleccionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ElectionReadinessCheck implements HealthCheck {

    @Inject
    EleccionRepository eleccionRepository;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("election-service-ready");
        try {
            eleccionRepository.count();
            return builder.up().build();
        } catch (Exception e) {
            return builder.down().withData("error", e.getMessage()).build();
        }
    }
}