package sisve.ec.election.mapper;

import sisve.ec.election.db.CandidatoEntity;
import sisve.ec.election.db.CargoEntity;
import sisve.ec.election.db.EleccionEntity;
import sisve.ec.election.dto.CandidatoRequest;
import sisve.ec.election.dto.CandidatoResponse;
import sisve.ec.election.dto.CargoRequest;
import sisve.ec.election.dto.CargoResponse;
import sisve.ec.election.dto.EleccionRequest;
import sisve.ec.election.dto.EleccionResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ElectionMapper {

    public EleccionEntity toEntity(EleccionRequest request) {
        EleccionEntity entity = new EleccionEntity();
        entity.nombre = request.nombre();
        entity.descripcion = request.descripcion();
        entity.fechaInicio = request.fechaInicio();
        entity.fechaFin = request.fechaFin();
        entity.estado = "planificado";
        return entity;
    }

    public EleccionResponse toResponse(EleccionEntity entity) {
        return new EleccionResponse(entity.idEleccion, entity.nombre, entity.descripcion, entity.fechaInicio, entity.fechaFin, entity.estado);
    }

    public CargoEntity toCargoEntity(Long idEleccion, CargoRequest request) {
        CargoEntity entity = new CargoEntity();
        entity.idEleccion = idEleccion;
        entity.nombre = request.nombre();
        return entity;
    }

    public CargoResponse toCargoResponse(CargoEntity entity) {
        return new CargoResponse(entity.idCargo, entity.idEleccion, entity.nombre);
    }

    public CandidatoEntity toCandidatoEntity(Long idCargo, CandidatoRequest request) {
        CandidatoEntity entity = new CandidatoEntity();
        entity.idCargo = idCargo;
        entity.nombres = request.nombres();
        entity.apellidos = request.apellidos();
        entity.lista = request.lista();
        entity.estado = true;
        return entity;
    }

    public CandidatoResponse toCandidatoResponse(CandidatoEntity entity) {
        return new CandidatoResponse(entity.idCandidato, entity.idCargo, entity.nombres, entity.apellidos, entity.lista, entity.estado);
    }

    public List<CandidatoResponse> toCandidatoResponseList(List<CandidatoEntity> entities) {
        return entities.stream().map(this::toCandidatoResponse).toList();
    }
}