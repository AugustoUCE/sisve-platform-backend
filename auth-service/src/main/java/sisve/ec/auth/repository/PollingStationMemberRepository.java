package sisve.ec.auth.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.auth.db.PollingStationMemberEntity;

import java.util.Optional;

@ApplicationScoped
public class PollingStationMemberRepository implements PanacheRepositoryBase<PollingStationMemberEntity, Long> {
    public Optional<PollingStationMemberEntity> findByUserIdentifier(String userIdentifier) {
        return find("userIdentifier = ?1", userIdentifier).firstResultOptional();
    }
}
