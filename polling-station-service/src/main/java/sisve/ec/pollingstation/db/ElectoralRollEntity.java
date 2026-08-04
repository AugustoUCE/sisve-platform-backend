package sisve.ec.pollingstation.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "electoral_roll", schema = "polling_station_schema")
public class ElectoralRollEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_electoral_roll")
    public Long idElectoralRoll;

    @Column(name = "id_polling_station", nullable = false)
    public Long idPollingStation;

    @Column(name = "id_election", nullable = false)
    public Long idElection;

    @Column(name = "id_voter", nullable = false)
    public Long idVoter;

    @Column(nullable = false, length = 20)
    public String cedula;

    @Column(name = "full_name", nullable = false, length = 200)
    public String fullName;

    @Column(name = "institutional_email", length = 150)
    public String institutionalEmail;

    @Column(name = "participation_status", nullable = false, length = 30)
    public String participationStatus = ParticipationStatus.PENDING;

    @Column(name = "enabled_at")
    public LocalDateTime enabledAt;

    @Column(name = "enabled_by", length = 50)
    public String enabledBy;

    @Column(name = "voted_at")
    public LocalDateTime votedAt;

    @Column(name = "blocked_at")
    public LocalDateTime blockedAt;

    @Column(name = "blocked_by", length = 50)
    public String blockedBy;

    @Column(name = "block_reason", length = 250)
    public String blockReason;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
