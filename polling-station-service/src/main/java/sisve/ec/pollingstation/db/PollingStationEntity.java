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
@Table(name = "polling_station", schema = "polling_station_schema")
public class PollingStationEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_polling_station")
    public Long idPollingStation;

    @Column(name = "id_election", nullable = false)
    public Long idElection;

    @Column(nullable = false, length = 30, unique = true)
    public String code;

    @Column(nullable = false, length = 150)
    public String name;

    @Column(length = 250)
    public String location;

    @Column(nullable = false, length = 30)
    public String status = PollingStationStatus.OPEN;

    @Column(name = "opened_at")
    public LocalDateTime openedAt;

    @Column(name = "closed_at")
    public LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
