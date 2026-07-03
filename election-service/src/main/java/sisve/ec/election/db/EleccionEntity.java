package com.uce.sisve.election.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eleccion", schema = "election_schema")
public class EleccionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_eleccion")
    public Long idEleccion;

    @Column(nullable = false, length = 120)
    public String nombre;

    @Column(columnDefinition = "TEXT")
    public String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    public LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    public LocalDateTime fechaFin;

    @Column(nullable = false, length = 20)
    public String estado = "planificado";
}