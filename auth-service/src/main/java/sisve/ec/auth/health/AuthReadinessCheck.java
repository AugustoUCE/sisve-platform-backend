package sisve.ec.auth.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AuthReadinessCheck implements HealthCheck {



    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse
                .named("auth-service-ready")
                .up()
                .withData("service", "auth-service")
                .withData("status", "ready")
                .build();
    }
}