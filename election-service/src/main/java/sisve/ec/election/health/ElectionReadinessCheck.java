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



    @Override
    public HealthCheckResponse call() {
       return HealthCheckResponse
                .named("election-service-ready")
                .up()
                .withData("service", "Election service is ready")
                .build();
    }
}