package sisve.ec.election.repository;

import sisve.ec.election.db.VotanteEleccionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import sisve.ec.election.db.VotanteEleccionEntity.VotanteEleccionId;
import java.time.LocalDateTime;

@ApplicationScoped
public class VotanteEleccionRepository implements PanacheRepositoryBase<VotanteEleccionEntity ,VotanteEleccionId> {

    public boolean estaHabilitado(Long idVotante, Long idEleccion) {
        return findByIdOptional(new VotanteEleccionId(idVotante, idEleccion))
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