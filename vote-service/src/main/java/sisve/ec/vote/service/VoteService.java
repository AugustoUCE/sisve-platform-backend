package com.uce.sisve.vote.service;

import com.uce.sisve.vote.client.AuditClient;
import com.uce.sisve.vote.client.AuthClient;
import com.uce.sisve.vote.client.ElectionClient;
import com.uce.sisve.vote.crypto.AesEncryptionUtil;
import com.uce.sisve.vote.crypto.Sha256ChainUtil;
import com.uce.sisve.vote.db.VotoEntity;
import com.uce.sisve.vote.dto.EventoAuditoriaDTO;
import com.uce.sisve.vote.dto.EleccionResponse;
import com.uce.sisve.vote.dto.VotoRequest;
import com.uce.sisve.vote.dto.VotoResponse;
import com.uce.sisve.vote.mapper.VotoMapper;
import com.uce.sisve.vote.repository.VotoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class VoteService {

    VotoRepository votoRepository;
    VotoMapper votoMapper;

    @RestClient
    AuthClient authClient;

    @RestClient
    ElectionClient electionClient;

    @RestClient
    AuditClient auditClient;

    AesEncryptionUtil aesUtil;
    Sha256ChainUtil sha256Util;
    MeterRegistry meterRegistry;

    @Transactional
    public VotoResponse emitirVoto(VotoRequest request, String authHeader) {
        validarPeriodoElectoral(request);

        Map<String, Boolean> validacion = authClient.validarToken(authHeader);
        if (!Boolean.TRUE.equals(validacion.get("valido"))) {
            throw httpException(Status.UNAUTHORIZED, "Token invalido");
        }

        Map<String, Boolean> habilitacion = electionClient.verificarHabilitado(request.idEleccion(), request.idVotante());
        if (!Boolean.TRUE.equals(habilitacion.get("habilitado"))) {
            throw httpException(Status.FORBIDDEN, "El votante no esta habilitado para esta eleccion");
        }

        String votoCifrado = aesUtil.cifrar(String.valueOf(request.idCandidato()));
        Optional<VotoEntity> ultimo = votoRepository.findUltimoVoto(request.idEleccion());
        String hashAnterior = ultimo.map(voto -> voto.hashActual).orElse(Sha256ChainUtil.HASH_SEMILLA);

        if (votoRepository.existeVotoParaEleccion(request.idEleccion(), hashAnterior)) {
            throw httpException(Status.CONFLICT, "Ya existe un voto registrado con el mismo encadenamiento");
        }

        String hashActual = sha256Util.calcularHash(votoCifrado, hashAnterior);
        VotoEntity voto = votoMapper.toEntity(request.idEleccion(), votoCifrado, hashAnterior, hashActual);
        votoRepository.persist(voto);

        electionClient.marcarVotado(request.idEleccion(), request.idVotante());
        authClient.logout(authHeader);
        auditClient.registrarEvento(new EventoAuditoriaDTO(
                "VOTO_EMITIDO",
                "Eleccion " + request.idEleccion(),
                "vote-service"
        ));

        meterRegistry.counter("vote.votos.emitidos", "eleccion", request.idEleccion().toString()).increment();
        meterRegistry.counter("vote.votos.emitidos.total").increment();

        return votoMapper.toResponse(voto);
    }

    public boolean verificarIntegridad(Long idEleccion) {
        List<VotoEntity> votos = votoRepository.findByEleccionOrdenado(idEleccion);
        boolean resultado = sha256Util.verificarCadena(votos);
        auditClient.registrarEvento(new EventoAuditoriaDTO(
                "INTEGRIDAD_VERIFICADA",
                "Resultado: " + resultado,
                "vote-service"
        ));
        meterRegistry.counter("vote.integridad.verificada", "eleccion", idEleccion.toString(), "resultado", Boolean.toString(resultado)).increment();
        return resultado;
    }

    private void validarPeriodoElectoral(VotoRequest request) {
        EleccionResponse eleccion = electionClient.getEleccion(request.idEleccion());
        LocalDateTime ahora = LocalDateTime.now();
        if (!"activo".equalsIgnoreCase(eleccion.estado())) {
            throw httpException(Status.FORBIDDEN, "La elección no se encuentra activa");
        }
        if (ahora.isBefore(eleccion.fechaInicio())) {
            throw httpException(Status.FORBIDDEN, "El período de votación aún no ha comenzado");
        }
        if (ahora.isAfter(eleccion.fechaFin())) {
            throw httpException(Status.FORBIDDEN, "El período de votación ha finalizado");
        }
    }

    private WebApplicationException httpException(Status status, String message) {
        return new WebApplicationException(Response.status(status).entity(message).build());
    }
}