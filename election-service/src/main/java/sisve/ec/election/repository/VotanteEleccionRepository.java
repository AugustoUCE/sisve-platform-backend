package com.uce.sisve.election.repository;

import com.uce.sisve.election.db.VotanteEleccionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDateTime;

@ApplicationScoped
public class VotanteEleccionRepository implements PanacheRepository<VotanteEleccionEntity> {

    public boolean estaHabilitado(Long idVotante, Long idEleccion) {
        return findByIdOptional(new VotanteEleccionEntity.VotanteEleccionId(idVotante, idEleccion))
                .map(votanteEleccion -> Boolean.FALSE.equals(votanteEleccion.haVotado))
                .orElse(false);
    }

    @Transactional
    public void marcarVotado(Long idVotante, Long idEleccion) {
        try {
            update("haVotado = true, fechaParticipacion = ?1 where id.idVotante = ?2 and id.idEleccion = ?3 and haVotado = false",
                    LocalDateTime.now(), idVotante, idEleccion);
        } catch (PersistenceException e) {
            throw new WebApplicationException("El votante ya emitió su voto en este proceso electoral", 409);
        }
    }
}