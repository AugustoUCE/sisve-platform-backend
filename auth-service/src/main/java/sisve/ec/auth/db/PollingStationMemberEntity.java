package sisve.ec.auth.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "miembro_mesa", schema = "auth_schema")
public class PollingStationMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_miembro_mesa")
    public Long idMiembroMesa;

    @Column(name = "user_identifier", nullable = false, unique = true, length = 50)
    public String userIdentifier;

    @Column(name = "password_hash", nullable = false, length = 128)
    public String passwordHash;

    @Column(name = "password_salt", nullable = false, length = 64)
    public String passwordSalt;

    @Column(name = "full_name", nullable = false, length = 200)
    public String fullName;

    @Column(name = "institutional_email", nullable = false, length = 150)
    public String institutionalEmail;

    @Column(name = "polling_station_id", nullable = false)
    public Long pollingStationId;

    @Column(nullable = false)
    public Boolean status = Boolean.TRUE;
}
