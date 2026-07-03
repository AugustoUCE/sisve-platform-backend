package com.uce.sisve.election.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cargo", schema = "election_schema")
public class CargoEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cargo")
    public Long idCargo;

    @Column(name = "id_eleccion", nullable = false)
    public Long idEleccion;

    @Column(nullable = false, length = 80)
    public String nombre;
}