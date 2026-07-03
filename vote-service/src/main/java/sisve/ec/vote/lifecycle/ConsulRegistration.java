package com.uce.sisve.vote.lifecycle;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ConsulRegistration {

    private static final Logger LOGGER = Logger.getLogger(ConsulRegistration.class);

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8083")
    int httpPort;

    @ConfigProperty(name = "consul.host", defaultValue = "localhost")
    String consulHost;

    @ConfigProperty(name = "consul.port", defaultValue = "8500")
    int consulPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    void onStart(@Observes StartupEvent event) {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String serviceId = "vote-service-" + hostName;
            String healthUrl = "http://localhost:" + httpPort + "/health/ready";
            String payload = "{" +
                    "\"ID\":\"" + serviceId + "\"," +
                    "\"Name\":\"vote-service\"," +
                    "\"Port\":" + httpPort + "," +
                    "\"Check\":{" +
                    "\"HTTP\":\"" + healthUrl + "\"," +
                    "\"Interval\":\"10s\"," +
                    "\"Timeout\":\"5s\"}" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + consulHost + ":" + consulPort + "/v1/agent/service/register"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.infof("Servicio registrado en Consul con id %s", serviceId);
            } else {
                LOGGER.warnf("No se pudo registrar el servicio en Consul. Status: %s", response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Error registrando vote-service en Consul", exception);
        } catch (Exception exception) {
            LOGGER.warn("Error registrando vote-service en Consul", exception);
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        LOGGER.info("Desregistración de Consul omitida; el agente expira la sesión por health check");
    }
}