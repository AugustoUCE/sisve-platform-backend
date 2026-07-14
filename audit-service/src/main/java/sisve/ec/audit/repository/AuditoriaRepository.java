package sisve.ec.audit.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.audit.db.AuditoriaEntity;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AuditoriaRepository implements PanacheRepositoryBase<AuditoriaEntity, Long > {

    public List<AuditoriaEntity> findByServicio(String servicioOrigen) {
        return list("servicioOrigen", servicioOrigen);
    }

    public List<AuditoriaEntity> findByServicioYFecha(String servicioOrigen, LocalDateTime desde) {
        return list("servicioOrigen = ?1 and fechaEvento >= ?2 order by fechaEvento asc", servicioOrigen, desde);
    }

    public List<AuditoriaEntity> findByTipoEvento(String tipoEvento) {
        return list("tipoEvento = ?1 order by fechaEvento desc", tipoEvento);
    }
}