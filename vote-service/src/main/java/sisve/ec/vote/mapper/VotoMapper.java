package sisve.ec.vote.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.vote.db.VotoEntity;
import sisve.ec.vote.dto.VotoResponse;

import java.time.LocalDateTime;

@ApplicationScoped
public class VotoMapper {

    public VotoEntity toEntity(Long idEleccion, String votoCifrado, String hashAnterior, String hashActual) {
        VotoEntity entity = new VotoEntity();
        entity.idEleccion = idEleccion;
        entity.votoCifrado = votoCifrado;
        entity.hashAnterior = hashAnterior;
        entity.hashActual = hashActual;
        entity.fechaRegistro = LocalDateTime.now();
        return entity;
    }

    public VotoResponse toResponse(Long idEleccion, Long idVotante, VotoEntity entity) {
        return new VotoResponse("Voto registrado correctamente", idEleccion, idVotante, entity.hashActual, entity.fechaRegistro);
    }
}