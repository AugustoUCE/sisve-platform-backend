package sisve.ec.pollingstation.repository;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import sisve.ec.pollingstation.db.ElectoralRollEntity;
import sisve.ec.pollingstation.db.ParticipationStatus;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

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

    public List<ElectoralRollEntity> searchByPollingStation(Long idPollingStation, String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return list("idPollingStation = ?1 and (lower(fullName) like ?2 or lower(cedula) like ?2 or lower(institutionalEmail) like ?2) order by fullName",
                idPollingStation, pattern);
    }

    public long enablePending(Long idPollingStation, Long idVoter, String enabledBy, LocalDateTime enabledAt) {
        return update("participationStatus = ?1, enabledAt = ?2, enabledBy = ?3, updatedAt = ?4 "
                        + "where idPollingStation = ?5 and idVoter = ?6 and participationStatus = ?7",
                ParticipationStatus.ENABLED, enabledAt, enabledBy, enabledAt,
                idPollingStation, idVoter, ParticipationStatus.PENDING);
    }

    public long countByPollingStation(Long idPollingStation) {
        return count("idPollingStation = ?1", idPollingStation);
    }

    public long countByPollingStationAndStatus(Long idPollingStation, String participationStatus) {
        return count("idPollingStation = ?1 and participationStatus = ?2", idPollingStation, participationStatus);
    }
}
