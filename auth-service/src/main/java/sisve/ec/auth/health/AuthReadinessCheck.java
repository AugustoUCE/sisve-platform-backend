package sisve.ec.auth.health;

import sisve.ec.auth.repository.VotanteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AuthReadinessCheck implements HealthCheck {

    @Inject
    VotanteRepository votanteRepository;

    @Override
    public HealthCheckResponse call() {
        try {
            votanteRepository.count();
            return HealthCheckResponse.up("auth-service-ready");
        } catch (Exception exception) {
            return HealthCheckResponse.builder()
                    .name("auth-service-ready")
                    .down()
                    .withData("error", exception.getMessage())
                    .build();
        }
    }
}