package sisve.ec.pollingstation.repository;

import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.pollingstation.db.PollingStationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PollingStationRepository implements PanacheRepositoryBase<PollingStationEntity, Long> {

    public List<PollingStationEntity> findByElection(Long idElection) {
        return list("idElection = ?1 order by code", idElection);
    }

    public Optional<PollingStationEntity> findByIdAndElection(Long idPollingStation, Long idElection) {
        return findByIdOptional(idPollingStation)
                .filter(station -> idElection.equals(station.idElection));
    }
}
