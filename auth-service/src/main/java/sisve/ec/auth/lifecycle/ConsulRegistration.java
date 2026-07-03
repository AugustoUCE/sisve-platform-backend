package sisve.ec.auth.lifecycle;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ConsulRegistration {

    private static final Logger LOGGER = Logger.getLogger(ConsulRegistration.class);

    @ConfigProperty(name = "consul.host")
    String consulHost;

    @ConfigProperty(name = "consul.port")
    int consulPort;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8081")
    int httpPort;

    void onStart(@Observes StartupEvent event) {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String serviceId = "auth-service-" + hostName;
            String healthUrl = "http://localhost:" + httpPort + "/health/ready";

            String payload = "{" +
                    "\"ID\":\"" + serviceId + "\"," +
                    "\"Name\":\"auth-service\"," +
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

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.infof("Servicio registrado en Consul con id %s", serviceId);
            } else {
                LOGGER.warnf("No se pudo registrar el servicio en Consul. Status: %s", response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Error registrando auth-service en Consul", exception);
        } catch (IOException exception) {
            LOGGER.warn("Error registrando auth-service en Consul", exception);
        } catch (Exception exception) {
            LOGGER.warn("Error inesperado registrando auth-service en Consul", exception);
        }
    }
}