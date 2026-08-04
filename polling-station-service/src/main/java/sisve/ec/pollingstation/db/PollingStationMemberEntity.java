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
@Table(name = "polling_station_member", schema = "polling_station_schema")
public class PollingStationMemberEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_polling_station_member")
    public Long idPollingStationMember;

    @Column(name = "id_polling_station", nullable = false)
    public Long idPollingStation;

    @Column(name = "user_identifier", nullable = false, length = 50)
    public String userIdentifier;

    @Column(name = "full_name", nullable = false, length = 200)
    public String fullName;

    @Column(name = "institutional_email", length = 150)
    public String institutionalEmail;

    @Column(nullable = false, length = 50)
    public String role;

    @Column(nullable = false)
    public Boolean status = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
