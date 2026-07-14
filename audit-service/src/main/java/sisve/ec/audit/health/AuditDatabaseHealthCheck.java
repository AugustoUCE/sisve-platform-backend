package sisve.ec.audit.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

@Liveness
@Readiness
@ApplicationScoped
public class AuditDatabaseHealthCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
                ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
                return HealthCheckResponse.up("audit-service-database");
            }
            return HealthCheckResponse.down("audit-service-database");
        } catch (Exception exception) {
            return HealthCheckResponse.named("audit-service-database")
                    .down()
                    .withData("error", exception.getMessage())
                    .build();
        }
    }
}