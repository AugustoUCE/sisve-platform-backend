package sisve.ec.auth.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesion", schema = "auth_schema")
public class SesionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    public Long idSesion;

    @Column(name = "id_votante", nullable = false)
    public Long idVotante;

    @Column(name = "token_hash", nullable = false, length = 256)
    public String tokenHash;

    @Column(name = "fecha_creacion", nullable = false)
    public LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    public LocalDateTime fechaExpiracion;

    @Column(name = "estado", nullable = false)
    public Boolean estado = Boolean.TRUE;
}