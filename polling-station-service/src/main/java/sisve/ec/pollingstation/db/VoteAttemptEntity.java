package sisve.ec.pollingstation.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "vote_attempt", schema = "polling_station_schema")
public class VoteAttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vote_attempt")
    public Long idVoteAttempt;

    @Column(name = "id_election", nullable = false)
    public Long idElection;

    @Column(name = "id_voter", nullable = false)
    public Long idVoter;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
