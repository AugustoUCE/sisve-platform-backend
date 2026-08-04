package sisve.ec.pollingstation.repository;

import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.pollingstation.db.PollingStationMemberEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PollingStationMemberRepository implements PanacheRepositoryBase<PollingStationMemberEntity, Long> {

    public List<PollingStationMemberEntity> findByPollingStation(Long idPollingStation) {
        return list("idPollingStation = ?1 order by role, fullName", idPollingStation);
    }

    public Optional<PollingStationMemberEntity> findPresident(Long idPollingStation) {
        return find("idPollingStation = ?1 and role = ?2 and status = true",
                idPollingStation,
                sisve.ec.pollingstation.db.PollingStationMemberRole.POLLING_STATION_PRESIDENT).firstResultOptional();
    }
}
