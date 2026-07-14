package sisve.ec.auth.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import sisve.ec.auth.db.VotanteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class VotanteRepository implements PanacheRepositoryBase<VotanteEntity, Long> {

    public Optional<VotanteEntity> findByCedulaAndCorreo(String cedula, String correo) {
        return find("cedula = ?1 and correoInstitucional = ?2", cedula, correo).firstResultOptional();
    }
}