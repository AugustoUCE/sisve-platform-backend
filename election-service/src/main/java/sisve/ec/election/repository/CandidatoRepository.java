package com.uce.sisve.election.repository;

import com.uce.sisve.election.db.CandidatoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CandidatoRepository implements PanacheRepository<CandidatoEntity> {

    public List<CandidatoEntity> findByCargo(Long idCargo) {
        return list("idCargo = ?1 and estado = true", idCargo);
    }

    public void deshabilitar(Long idCandidato) {
        update("estado = false where idCandidato = ?1", idCandidato);
    }
}