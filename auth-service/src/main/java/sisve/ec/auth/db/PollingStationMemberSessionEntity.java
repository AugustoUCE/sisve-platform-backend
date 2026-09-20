package sisve.ec.auth.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_miembro_mesa", schema = "auth_schema")
public class PollingStationMemberSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion_miembro_mesa")
    public Long idSesionMiembroMesa;

    @Column(name = "id_miembro_mesa", nullable = false)
    public Long idMiembroMesa;

    @Column(name = "token_hash", nullable = false, unique = true, length = 256)
    public String tokenHash;

    @Column(name = "fecha_expiracion", nullable = false)
    public LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    public Boolean estado = Boolean.TRUE;
}
