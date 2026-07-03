package com.uce.sisve.election.service;

import com.uce.sisve.election.client.AuditClient;
import com.uce.sisve.election.db.CandidatoEntity;
import com.uce.sisve.election.db.CargoEntity;
import com.uce.sisve.election.db.EleccionEntity;
import com.uce.sisve.election.db.VotanteEleccionEntity;
import com.uce.sisve.election.dto.CandidatoRequest;
import com.uce.sisve.election.dto.CandidatoResponse;
import com.uce.sisve.election.dto.CargoRequest;
import com.uce.sisve.election.dto.CargoResponse;
import com.uce.sisve.election.dto.EleccionRequest;
import com.uce.sisve.election.dto.EleccionResponse;
import com.uce.sisve.election.dto.EventoAuditoriaDTO;
import com.uce.sisve.election.dto.PadronCargaRequest;
import com.uce.sisve.election.mapper.ElectionMapper;
import com.uce.sisve.election.repository.CandidatoRepository;
import com.uce.sisve.election.repository.CargoRepository;
import com.uce.sisve.election.repository.EleccionRepository;
import com.uce.sisve.election.repository.VotanteEleccionRepository;
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