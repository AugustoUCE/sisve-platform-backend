package com.uce.sisve.vote.repository;

import com.uce.sisve.vote.db.VotoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VotoRepository implements PanacheRepository<VotoEntity> {

    public Optional<VotoEntity> findUltimoVoto(Long idEleccion) {
        return find("idEleccion = ?1 order by fechaRegistro desc", idEleccion).firstResultOptional();
    }

    public List<VotoEntity> findByEleccionOrdenado(Long idEleccion) {
        return list("idEleccion = ?1 order by fechaRegistro asc", idEleccion);
    }

    public boolean existeVotoParaEleccion(Long idEleccion, String hashAnterior) {
        return count("idEleccion = ?1 and hashAnterior = ?2", idEleccion, hashAnterior) > 0;
    }
}