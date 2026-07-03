package com.uce.sisve.election.lifecycle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ConsulRegistration {

    private static final Logger LOG = Logger.getLogger(ConsulRegistration.class);

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8082")
    int httpPort;

    @ConfigProperty(name = "consul.host", defaultValue = "localhost")
    String consulHost;

    @ConfigProperty(name = "consul.port", defaultValue = "8500")
    int consulPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    void onStart(@Observes StartupEvent event) {
        try {
            String payload = "{\"Name\":\"election-service\",\"Port\":" + httpPort + ",\"Check\":{\"HTTP\":\"http://localhost:" + httpPort + "/health/ready\",\"Interval\":\"10s\"}}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + consulHost + ":" + consulPort + "/v1/agent/service/register"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            LOG.info("Servicio election-service registrado en Consul");
        } catch (Exception e) {
            LOG.warn("No fue posible registrar election-service en Consul", e);
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        LOG.info("Desregistración de Consul omitida; el agente expira la sesión por health check");
    }
}