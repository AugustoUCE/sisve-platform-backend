package sisve.ec.auth.repository;

import sisve.ec.auth.db.SesionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class SesionRepository implements PanacheRepository<SesionEntity> {

    public Optional<SesionEntity> findByTokenHash(String tokenHash) {
        return find("tokenHash = ?1 and estado = true", tokenHash).firstResultOptional();
    }

    @Transactional
    public void invalidarSesion(Long idSesion) {
        update("estado = false where idSesion = ?1", idSesion);
    }
}