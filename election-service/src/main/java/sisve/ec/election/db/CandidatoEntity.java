package com.uce.sisve.election.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "candidato", schema = "election_schema")
public class CandidatoEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_candidato")
    public Long idCandidato;

    @Column(name = "id_cargo", nullable = false)
    public Long idCargo;

    @Column(nullable = false, length = 80)
    public String nombres;

    @Column(nullable = false, length = 80)
    public String apellidos;

    @Column(length = 50)
    public String lista;

    @Column(nullable = false)
    public Boolean estado = true;
}