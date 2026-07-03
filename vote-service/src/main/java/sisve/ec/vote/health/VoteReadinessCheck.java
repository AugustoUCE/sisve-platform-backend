package com.uce.sisve.vote.health;

import com.uce.sisve.vote.crypto.AesEncryptionUtil;
import com.uce.sisve.vote.repository.VotoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

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
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("vote-service-ready");
        try {
            votoRepository.count();
            if (aesKeyBase64 == null || aesKeyBase64.isBlank() || !aesEncryptionUtil.estaConfigurado()) {
                return builder.down().withData("error", "La clave AES no esta configurada").build();
            }
            return builder.up().build();
        } catch (Exception exception) {
            return builder.down().withData("error", exception.getMessage()).build();
        }
    }
}