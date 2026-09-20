package sisve.ec.auth.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.auth.db.PollingStationMemberSessionEntity;

import java.util.Optional;

@ApplicationScoped
public class PollingStationMemberSessionRepository implements PanacheRepositoryBase<PollingStationMemberSessionEntity, Long> {
    public Optional<PollingStationMemberSessionEntity> findByTokenHash(String tokenHash) {
        return find("tokenHash = ?1 and estado = true", tokenHash).firstResultOptional();
    }
}
