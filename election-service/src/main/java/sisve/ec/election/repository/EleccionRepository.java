package com.uce.sisve.election.repository;

import com.uce.sisve.election.db.EleccionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class EleccionRepository implements PanacheRepository<EleccionEntity> {

    public List<EleccionEntity> findActivas() {
        LocalDateTime ahora = LocalDateTime.now();
        return list("estado = ?1 and fechaInicio <= ?2 and fechaFin >= ?2", "activo", ahora);
    }
}