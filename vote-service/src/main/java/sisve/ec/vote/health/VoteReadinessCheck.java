package sisve.ec.vote.health;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import sisve.ec.vote.crypto.AesEncryptionUtil;
import sisve.ec.vote.repository.VotoRepository;


@Readiness
@ApplicationScoped
public class VoteReadinessCheck implements HealthCheck {

    @Inject
    VotoRepository votoRepository;

    @Inject
    AesEncryptionUtil aesEncryptionUtil;

    @ConfigProperty(name = "vote.aes.secret.key")
    String aesKeyBase64;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse
                .named("vote-service-ready")
                .up()
                .withData("service", "Vote service is ready")
                .build();
    }
}