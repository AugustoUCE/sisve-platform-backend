package sisve.ec.election.service;

import sisve.ec.election.client.AuditClient;
import sisve.ec.election.db.CandidatoEntity;
import sisve.ec.election.db.CargoEntity;
import sisve.ec.election.db.EleccionEntity;
import sisve.ec.election.db.VotanteEleccionEntity;
import sisve.ec.election.dto.CandidatoRequest;
import sisve.ec.election.dto.EventoAuditoriaDTO;
import sisve.ec.election.dto.CandidatoResponse;
import sisve.ec.election.dto.CargoResponse;
import sisve.ec.election.dto.EstadoParticipacionResponse;
import sisve.ec.election.dto.EleccionRequest;
import sisve.ec.election.dto.EleccionResponse;
import sisve.ec.election.dto.PadronCargaRequest;
import sisve.ec.election.dto.CargoRequest;
import sisve.ec.election.mapper.ElectionMapper;
import sisve.ec.election.repository.CandidatoRepository;
import sisve.ec.election.repository.CargoRepository;
import sisve.ec.election.repository.EleccionRepository;
import sisve.ec.election.repository.VotanteEleccionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
public class ElectionService {

    @Inject
    EleccionRepository eleccionRepository;

    @Inject
    CargoRepository cargoRepository;

    @Inject
    CandidatoRepository candidatoRepository;

    @Inject
    VotanteEleccionRepository votanteEleccionRepository;

    @Inject
    ElectionMapper mapper;

    @Inject
    @RestClient
    AuditClient auditClient;

    @Inject
    MeterRegistry meterRegistry;

    @Transactional
    public EleccionResponse crearEleccion(EleccionRequest request) {
        EleccionEntity entity = mapper.toEntity(request);
        eleccionRepository.persist(entity);
        auditClient.registrarEvento(new EventoAuditoriaDTO("ELECCION_CREADA", "Elección " + entity.nombre, "election-service"));
        meterRegistry.counter("election.elecciones.creadas").increment();
        return mapper.toResponse(entity);
    }

    public List<EleccionResponse> listarActivas() {
        return eleccionRepository.findActivas().stream().map(mapper::toResponse).toList();
    }

    public EleccionResponse obtenerEleccion(Long idEleccion) {
        EleccionEntity eleccion = eleccionRepository.findById(idEleccion);
        if (eleccion == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return mapper.toResponse(eleccion);
    }

    public List<CargoResponse> listarCargosPorEleccion(Long idEleccion) {
        EleccionEntity eleccion = eleccionRepository.findById(idEleccion);
        if (eleccion == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return cargoRepository.findByEleccion(idEleccion).stream()
                .map(mapper::toCargoResponse)
                .toList();
    }

    public List<CandidatoResponse> listarCandidatosPorEleccion(Long idEleccion) {
        EleccionEntity eleccion = eleccionRepository.findById(idEleccion);
        if (eleccion == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        List<CargoEntity> cargos = cargoRepository.findByEleccion(idEleccion);
        return cargos.stream()
                .flatMap(cargo -> candidatoRepository.findByCargo(cargo.idCargo).stream())
                .map(mapper::toCandidatoResponse)
                .toList();
    }

    public EstadoParticipacionResponse obtenerEstadoParticipacion(Long idEleccion, Long idVotante) {
        boolean habilitado = votanteEleccionRepository.estaHabilitado(idVotante, idEleccion);
        boolean haVotado = votanteEleccionRepository.findByIdOptional(new VotanteEleccionEntity.VotanteEleccionId(idVotante, idEleccion))
                .map(registro -> Boolean.TRUE.equals(registro.haVotado))
                .orElse(false);
        return new EstadoParticipacionResponse(idEleccion, idVotante, habilitado, haVotado);
    }

    @Transactional
    public CargoResponse crearCargo(Long idEleccion, CargoRequest request) {
        EleccionEntity eleccion = eleccionRepository.findById(idEleccion);
        if (eleccion == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        CargoEntity entity = mapper.toCargoEntity(idEleccion, request);
        cargoRepository.persist(entity);
        auditClient.registrarEvento(new EventoAuditoriaDTO("CARGO_CREADO", "Cargo " + entity.nombre, "election-service"));
        return mapper.toCargoResponse(entity);
    }

    @Transactional
    public CandidatoResponse crearCandidato(Long idCargo, CandidatoRequest request) {
        CargoEntity cargo = cargoRepository.findById(idCargo);
        if (cargo == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        CandidatoEntity entity = mapper.toCandidatoEntity(idCargo, request);
        candidatoRepository.persist(entity);
        auditClient.registrarEvento(new EventoAuditoriaDTO("CANDIDATO_REGISTRADO", entity.nombres + " " + entity.apellidos, "election-service"));
        return mapper.toCandidatoResponse(entity);
    }

    public List<CandidatoResponse> listarCandidatosPorCargo(Long idCargo) {
        return mapper.toCandidatoResponseList(candidatoRepository.findByCargo(idCargo));
    }

    @Transactional
    public void cargarPadron(Long idEleccion, PadronCargaRequest request) {
        EleccionEntity eleccion = eleccionRepository.findById(idEleccion);
        if (eleccion == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        List<VotanteEleccionEntity> registros = request.idsVotantes().stream()
                .map(idVotante -> {
                    VotanteEleccionEntity entity = new VotanteEleccionEntity();
                    entity.id = new VotanteEleccionEntity.VotanteEleccionId(idVotante, idEleccion);
                    entity.haVotado = false;
                    return entity;
                })
                .toList();
        votanteEleccionRepository.persist(registros);
        auditClient.registrarEvento(new EventoAuditoriaDTO("PADRON_CARGADO", "Cantidad de votantes: " + registros.size(), "election-service"));
    }

    public boolean verificarHabilitado(Long idEleccion, Long idVotante) {
        return votanteEleccionRepository.estaHabilitado(idVotante, idEleccion);
    }

    @Transactional
    public void marcarVotado(Long idEleccion, Long idVotante) {
        if (!verificarHabilitado(idEleccion, idVotante)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        votanteEleccionRepository.marcarVotado(idVotante, idEleccion);
        auditClient.registrarEvento(new EventoAuditoriaDTO("PARTICIPACION_REGISTRADA", "Votante " + idVotante + " participó en la elección " + idEleccion, "election-service"));
        meterRegistry.counter("election.votos.marcados").increment();
    }
}