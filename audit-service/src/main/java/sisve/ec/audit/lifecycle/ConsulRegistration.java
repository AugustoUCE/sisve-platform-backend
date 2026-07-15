package sisve.ec.audit.lifecycle;

import io.quarkus.runtime.ShutdownEvent;
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

    private static final Logger LOGGER =
            Logger.getLogger(ConsulRegistration.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String serviceId;

    @ConfigProperty(
            name = "quarkus.http.port",
            defaultValue = "8084"
    )
    int httpPort;

    @ConfigProperty(
            name = "consul.host",
            defaultValue = "localhost"
    )
    String consulHost;

    @ConfigProperty(
            name = "consul.port",
            defaultValue = "8500"
    )
    int consulPort;

    @ConfigProperty(
            name = "service.discovery.address",
            defaultValue = "host.docker.internal"
    )
    String serviceAddress;

    void onStart(@Observes StartupEvent event) {
        registerService();
    }

    void onStop(@Observes ShutdownEvent event) {
        deregisterService();
    }

    private void registerService() {
        try {
            String hostName = InetAddress
                    .getLocalHost()
                    .getHostName();

            serviceId = "audit-service-" + hostName;

            String healthUrl = String.format(
                    "http://%s:%d/health/ready",
                    serviceAddress,
                    httpPort
            );

            String payload = """
                    {
                      "ID": "%s",
                      "Name": "audit-service",
                      "Address": "%s",
                      "Port": %d,
                      "Tags": [
                        "sisve",
                        "audit",
                        "quarkus"
                      ],
                      "Check": {
                        "Name": "audit-service-readiness",
                        "HTTP": "%s",
                        "Interval": "10s",
                        "Timeout": "5s",
                        "DeregisterCriticalServiceAfter": "1m"
                      }
                    }
                    """.formatted(
                    serviceId,
                    serviceAddress,
                    httpPort,
                    healthUrl
            );

            URI registrationUri = URI.create(
                    "http://"
                            + consulHost
                            + ":"
                            + consulPort
                            + "/v1/agent/service/register"
                            + "?replace-existing-checks=true"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(registrationUri)
                    .header("Content-Type", "application/json")
                    .PUT(
                            HttpRequest.BodyPublishers.ofString(
                                    payload,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {

                LOGGER.infof(
                        "audit-service registrado en Consul. "
                                + "ID: %s, dirección: %s:%d, health: %s",
                        serviceId,
                        serviceAddress,
                        httpPort,
                        healthUrl
                );

                return;
            }

            LOGGER.warnf(
                    "Consul rechazó el registro de audit-service. "
                            + "Status: %d, respuesta: %s",
                    response.statusCode(),
                    response.body()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            LOGGER.warn(
                    "Registro de audit-service interrumpido",
                    exception
            );

        } catch (IOException exception) {
            LOGGER.warn(
                    "No fue posible comunicarse con Consul",
                    exception
            );

        } catch (Exception exception) {
            LOGGER.warn(
                    "Error inesperado registrando audit-service",
                    exception
            );
        }
    }

    private void deregisterService() {
        if (serviceId == null || serviceId.isBlank()) {
            return;
        }

        try {
            URI deregistrationUri = URI.create(
                    "http://"
                            + consulHost
                            + ":"
                            + consulPort
                            + "/v1/agent/service/deregister/"
                            + serviceId
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(deregistrationUri)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {

                LOGGER.infof(
                        "audit-service eliminado de Consul. ID: %s",
                        serviceId
                );

                return;
            }

            LOGGER.warnf(
                    "No se pudo eliminar audit-service de Consul. "
                            + "Status: %d, respuesta: %s",
                    response.statusCode(),
                    response.body()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            LOGGER.warn(
                    "Desregistro de audit-service interrumpido",
                    exception
            );

        } catch (Exception exception) {
            LOGGER.warn(
                    "No se pudo eliminar audit-service de Consul",
                    exception
            );
        }
    }
}