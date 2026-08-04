package sisve.ec.pollingstation.repository;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import sisve.ec.pollingstation.db.ElectoralRollEntity;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ElectoralRollRepository implements PanacheRepositoryBase<ElectoralRollEntity, Long> {

    public List<ElectoralRollEntity> findByPollingStation(Long idPollingStation) {
        return list("idPollingStation = ?1 order by fullName", idPollingStation);
    }

    public Optional<ElectoralRollEntity> findByPollingStationAndCedula(Long idPollingStation, String cedula) {
        return find("idPollingStation = ?1 and cedula = ?2", idPollingStation, cedula).firstResultOptional();
    }

    public Optional<ElectoralRollEntity> findByElectionAndVoter(Long idElection, Long idVoter) {
        return find("idElection = ?1 and idVoter = ?2", idElection, idVoter).firstResultOptional();
    }

    public Optional<ElectoralRollEntity> findByPollingStationAndVoter(Long idPollingStation, Long idVoter) {
        return find("idPollingStation = ?1 and idVoter = ?2", idPollingStation, idVoter).firstResultOptional();
    }

    public long countByPollingStation(Long idPollingStation) {
        return count("idPollingStation = ?1", idPollingStation);
    }

    public long countByPollingStationAndStatus(Long idPollingStation, String participationStatus) {
        return count("idPollingStation = ?1 and participationStatus = ?2", idPollingStation, participationStatus);
    }
}
