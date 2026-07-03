package sisve.ec.audit.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.audit.db.AuditoriaEntity;
import sisve.ec.audit.dto.EventoAuditoriaRequest;
import sisve.ec.audit.dto.EventoAuditoriaResponse;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AuditoriaMapper {

    public AuditoriaEntity toEntity(EventoAuditoriaRequest request) {
        AuditoriaEntity entity = new AuditoriaEntity();
        entity.tipoEvento = request.tipoEvento();
        entity.descripcion = request.descripcion();
        entity.ipOrigen = request.ipOrigen();
        entity.servicioOrigen = request.servicioOrigen();
        entity.fechaEvento = LocalDateTime.now();
        return entity;
    }

    public EventoAuditoriaResponse toResponse(AuditoriaEntity entity) {
        return new EventoAuditoriaResponse(
                entity.idAuditoria,
                entity.tipoEvento,
                entity.descripcion,
                entity.fechaEvento,
                entity.ipOrigen,
                entity.servicioOrigen);
    }

    public List<EventoAuditoriaResponse> toResponseList(List<AuditoriaEntity> entities) {
        return entities.stream().map(this::toResponse).toList();
    }
}