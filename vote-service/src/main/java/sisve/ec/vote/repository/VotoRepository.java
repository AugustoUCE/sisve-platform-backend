package sisve.ec.vote.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.vote.db.VotoEntity;

import java.util.List;
import java.util.Optional;

import static io.quarkus.hibernate.orm.panache.PanacheEntityBase.count;
import static io.quarkus.hibernate.orm.panache.PanacheEntityBase.find;
import static java.util.Collections.list;

@ApplicationScoped
public class VotoRepository implements PanacheRepositoryBase<VotoEntity, Long> {

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