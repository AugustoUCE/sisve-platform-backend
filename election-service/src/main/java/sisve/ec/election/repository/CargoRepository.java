package sisve.ec.election.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import sisve.ec.election.db.CargoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CargoRepository implements PanacheRepositoryBase<CargoEntity,Long> {

    public List<CargoEntity> findByEleccion(Long idEleccion) {
        return list("idEleccion", idEleccion);
    }
}