package com.uce.sisve.election.repository;

import com.uce.sisve.election.db.CargoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CargoRepository implements PanacheRepository<CargoEntity> {

    public List<CargoEntity> findByEleccion(Long idEleccion) {
        return list("idEleccion", idEleccion);
    }
}