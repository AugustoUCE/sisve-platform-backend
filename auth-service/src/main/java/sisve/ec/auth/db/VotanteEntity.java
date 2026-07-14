package sisve.ec.auth.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "votante", schema = "auth_schema")
public class VotanteEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_votante")
    public Long idVotante;

    @Column(name = "cedula", nullable = false, unique = true, length = 10)
    public String cedula;

    @Column(name = "correo_institucional", nullable = false, unique = true, length = 100)
    public String correoInstitucional;

    @Column(name = "nombres", nullable = false, length = 80)
    public String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    public String apellidos;

    @Column(name = "estado", nullable = false)
    public Boolean estado = Boolean.TRUE;
}