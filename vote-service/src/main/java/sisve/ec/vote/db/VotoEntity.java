package sisve.ec.vote.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "voto", schema = "vote_schema")
public class VotoEntity   {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_voto")
    public Long idVoto;

    @Column(name = "id_eleccion", nullable = false)
    public Long idEleccion;

    @Column(name = "voto_cifrado", nullable = false, columnDefinition = "TEXT")
    public String votoCifrado;

    @Column(name = "hash_anterior", nullable = false, length = 64)
    public String hashAnterior;

    @Column(name = "hash_actual", nullable = false, length = 64, unique = true)
    public String hashActual;

    @Column(name = "fecha_registro", nullable = false)
    public LocalDateTime fechaRegistro;
}