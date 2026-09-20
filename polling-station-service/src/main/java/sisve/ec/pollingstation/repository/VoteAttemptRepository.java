package sisve.ec.pollingstation.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.pollingstation.db.VoteAttemptEntity;

import java.util.Optional;

@ApplicationScoped
public class VoteAttemptRepository implements PanacheRepositoryBase<VoteAttemptEntity, Long> {
    public Optional<VoteAttemptEntity> findByElectionAndVoter(Long idElection, Long idVoter) {
        return find("idElection = ?1 and idVoter = ?2", idElection, idVoter).firstResultOptional();
    }
}
