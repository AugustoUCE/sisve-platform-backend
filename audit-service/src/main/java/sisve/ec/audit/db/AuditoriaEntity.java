package sisve.ec.audit.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria", schema = "audit_schema")
public class AuditoriaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    public Long idAuditoria;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    public String tipoEvento;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    public String descripcion;

    @Column(name = "fecha_evento", nullable = false)
    public LocalDateTime fechaEvento;

    @Column(name = "ip_origen", length = 45)
    public String ipOrigen;

    @Column(name = "servicio_origen", nullable = false, length = 50)
    public String servicioOrigen;
}