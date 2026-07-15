package sisve.ec.auth.lifecycle;

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
            name = "quarkus.http.port",
            defaultValue = "8081"
    )
    int httpPort;

    /*
     * Dirección que usarán Consul y los demás contenedores
     * para acceder al microservicio ejecutado en Windows.
     */
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

            serviceId = "auth-service-" + hostName;

            String healthUrl = String.format(
                    "http://%s:%d/health/ready",
                    serviceAddress,
                    httpPort
            );

            String payload = """
                    {
                      "ID": "%s",
                      "Name": "auth-service",
                      "Address": "%s",
                      "Port": %d,
                      "Tags": [
                        "sisve",
                        "auth",
                        "quarkus"
                      ],
                      "Check": {
                        "Name": "auth-service-readiness",
                        "HTTP": "%s",
                        "Interval": "10s",
                        "Timeout": "5s"
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
                        "Servicio registrado en Consul. "
                                + "ID: %s, dirección: %s, health: %s",
                        serviceId,
                        serviceAddress,
                        healthUrl
                );

                return;
            }

            LOGGER.warnf(
                    "Consul rechazó el registro. "
                            + "Status: %d, respuesta: %s",
                    response.statusCode(),
                    response.body()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            LOGGER.warn(
                    "Registro de auth-service interrumpido",
                    exception
            );

        } catch (IOException exception) {
            LOGGER.warn(
                    "No fue posible comunicarse con Consul",
                    exception
            );

        } catch (Exception exception) {
            LOGGER.warn(
                    "Error inesperado registrando auth-service",
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
                        "Servicio eliminado de Consul. ID: %s",
                        serviceId
                );

                return;
            }

            LOGGER.warnf(
                    "Consul no pudo eliminar el servicio. "
                            + "Status: %d, respuesta: %s",
                    response.statusCode(),
                    response.body()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            LOGGER.warn(
                    "Eliminación de auth-service interrumpida",
                    exception
            );

        } catch (Exception exception) {
            LOGGER.warn(
                    "No se pudo eliminar auth-service de Consul",
                    exception
            );
        }
    }
}