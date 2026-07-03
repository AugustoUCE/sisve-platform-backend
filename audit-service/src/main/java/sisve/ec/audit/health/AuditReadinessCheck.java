package sisve.ec.audit.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import sisve.ec.audit.repository.AuditoriaRepository;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AuditReadinessCheck implements HealthCheck {

    @Inject
    AuditoriaRepository auditoriaRepository;

    @Override
    public HealthCheckResponse call() {
        try {
            auditoriaRepository.count();
            return HealthCheckResponse.up("audit-service-ready");
        } catch (Exception exception) {
            return HealthCheckResponse.down("audit-service-ready").withData("error", exception.getMessage()).build();
        }
    }
}