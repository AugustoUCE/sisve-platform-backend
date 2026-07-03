package com.uce.sisve.vote.lifecycle;

import com.uce.sisve.vote.crypto.AesEncryptionUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VoteStartup {

    private static final Logger LOGGER = Logger.getLogger(VoteStartup.class);

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    AesEncryptionUtil aesEncryptionUtil;

    @ConfigProperty(name = "vote.aes.secret.key")
    String aesKeyBase64;

    void onStart(@Observes StartupEvent event) {
        meterRegistry.counter("vote_service_startup_total").increment();
        if (aesKeyBase64 != null && !aesKeyBase64.isBlank() && aesEncryptionUtil.estaConfigurado()) {
            LOGGER.info("vote-service iniciado con clave AES configurada");
        } else {
            LOGGER.warn("vote-service iniciado sin clave AES configurada");
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        meterRegistry.counter("vote_service_shutdown_total").increment();
        LOGGER.info("vote-service detenido");
    }
}