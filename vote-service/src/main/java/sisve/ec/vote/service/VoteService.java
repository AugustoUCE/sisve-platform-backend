package sisve.ec.vote.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import sisve.ec.vote.client.AuditClient;
import sisve.ec.vote.client.CargoClient;
import sisve.ec.vote.client.PollingStationClient;
import sisve.ec.vote.client.AuthClient;
import sisve.ec.vote.client.ElectionClient;
import sisve.ec.vote.crypto.AesEncryptionUtil;
import sisve.ec.vote.crypto.Sha256ChainUtil;
import sisve.ec.vote.db.VotoEntity;
import sisve.ec.vote.dto.CandidatoResponse;
import sisve.ec.vote.dto.CargoResponse;
import sisve.ec.vote.dto.EleccionResponse;
import sisve.ec.vote.dto.EventoAuditoriaDTO;
import sisve.ec.vote.dto.VotoRequest;
import sisve.ec.vote.dto.VotoResponse;
import sisve.ec.vote.dto.ValidateResponse;
import sisve.ec.vote.dto.EstadoParticipacionResponse;
import sisve.ec.vote.mapper.VotoMapper;
import sisve.ec.vote.repository.VotoRepository;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VoteService {

    private static final Logger LOGGER = Logger.getLogger(VoteService.class);

    @Inject
    VotoRepository votoRepository;

    @Inject
    VotoMapper votoMapper;

    @Inject
    @RestClient
    AuthClient authClient;

    @Inject
    @RestClient
    ElectionClient electionClient;

    @Inject
    @RestClient
    CargoClient cargoClient;

    @Inject
    @RestClient
    PollingStationClient pollingStationClient;

    @Inject
    @RestClient
    AuditClient auditClient;

    @Inject
    AesEncryptionUtil aesUtil;

    @Inject
    Sha256ChainUtil sha256Util;

    @Inject
    MeterRegistry meterRegistry;

    @Transactional
    public VotoResponse emitirVoto(VotoRequest request, String authHeader) {
        ValidateResponse validacion = authClient.validarToken(authHeader);
        if (!Boolean.TRUE.equals(validacion.valido())) {
            throw httpException(Status.UNAUTHORIZED, "Token invalido");
        }

        if (!request.idVotante().equals(validacion.idVotante())) {
            throw httpException(Status.FORBIDDEN, "El votante autenticado no coincide con el cuerpo de la solicitud");
        }

        EleccionResponse eleccion = electionClient.getEleccion(request.idEleccion());
        validarPeriodoElectoral(eleccion);

        // Verificar elegibilidad en la mesa asignada
        Boolean elegible;
        try {
            var eleg = pollingStationClient.validarElegibilidad(request.idEleccion(), request.idVotante());
            elegible = null;
            if (eleg != null && eleg.get("eligible") instanceof Boolean) {
                elegible = (Boolean) eleg.get("eligible");
            }
        } catch (Exception ex) {
            LOGGER.warnf(ex, "No fue posible verificar elegibilidad en polling-station-service");
            throw httpException(Status.SERVICE_UNAVAILABLE, "No se puede verificar elegibilidad en este momento");
        }
        if (!Boolean.TRUE.equals(elegible)) {
            registrarAuditoriaSegura("VOTO_NO_HABILITADO_MESA", "Votante no elegible en la mesa para la elección " + request.idEleccion(), "vote-service");
            throw httpException(Status.FORBIDDEN, "El votante no es elegible en su mesa de votación");
        }

        validarCargoPerteneceAEleccion(request.idEleccion(), request.idCargo());
        validarCandidatoPerteneceACargo(request.idCargo(), request.idCandidato());

        EstadoParticipacionResponse estadoParticipacion = electionClient.obtenerEstadoParticipacion(request.idEleccion(), request.idVotante());
        if (Boolean.TRUE.equals(estadoParticipacion.haVotado())) {
            registrarAuditoriaSegura("VOTO_DUPLICADO", "Intento de voto duplicado para la elección " + request.idEleccion(), "vote-service");
            throw httpException(Status.CONFLICT, "El votante ya registró su voto en esta elección.");
        }

        if (!Boolean.TRUE.equals(estadoParticipacion.habilitado())) {
            throw httpException(Status.FORBIDDEN, "El votante no esta habilitado para esta eleccion");
        }

        String votoCifrado = aesUtil.cifrar(String.valueOf(request.idCandidato()));
        Optional<VotoEntity> ultimo = votoRepository.findUltimoVoto(request.idEleccion());
        String hashAnterior = ultimo.map(voto -> voto.hashActual).orElse(Sha256ChainUtil.HASH_SEMILLA);

        String hashActual = sha256Util.calcularHash(votoCifrado, hashAnterior);
        VotoEntity voto = votoMapper.toEntity(request.idEleccion(), votoCifrado, hashAnterior, hashActual);
        votoRepository.persist(voto);

        electionClient.marcarVotado(request.idEleccion(), request.idVotante());
        // Intentar marcar votado en el servicio de mesas; falla no debe revertir el voto ya persistido
        try {
            pollingStationClient.marcarVotado(request.idEleccion(), request.idVotante());
        } catch (Exception ex) {
            LOGGER.warnf(ex, "No fue posible marcar votado en polling-station-service para votante %s en eleccion %s", request.idVotante(), request.idEleccion());
        }
        authClient.logout(authHeader);
        registrarAuditoriaSegura("VOTO_EMITIDO", "Eleccion " + request.idEleccion() + ", votante " + request.idVotante(), "vote-service");

        registrarMetricaSegura(() -> meterRegistry.counter("vote.votos.emitidos", "eleccion", request.idEleccion().toString()).increment(),
            "vote.votos.emitidos");
        return votoMapper.toResponse(request.idEleccion(), request.idVotante(), voto);
    }

    public boolean verificarIntegridad(Long idEleccion) {
        List<VotoEntity> votos = votoRepository.findByEleccionOrdenado(idEleccion);
        boolean resultado = sha256Util.verificarCadena(votos);
        registrarAuditoriaSegura("INTEGRIDAD_VERIFICADA", "Resultado: " + resultado, "vote-service");
        registrarMetricaSegura(() -> meterRegistry.counter("vote.integridad.verificada", "eleccion", idEleccion.toString(), "resultado", Boolean.toString(resultado)).increment(),
            "vote.integridad.verificada");
        return resultado;
    }

    private void validarPeriodoElectoral(EleccionResponse eleccion) {
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

    private void validarCargoPerteneceAEleccion(Long idEleccion, Long idCargo) {
        List<CargoResponse> cargos = electionClient.getCargos(idEleccion);
        boolean cargoValido = cargos.stream().anyMatch(cargo -> idCargo.equals(cargo.idCargo()));
        if (!cargoValido) {
            throw httpException(Status.NOT_FOUND, "El cargo no pertenece a la elección indicada");
        }
    }

    private void validarCandidatoPerteneceACargo(Long idCargo, Long idCandidato) {
        List<CandidatoResponse> candidatos = cargoClient.getCandidatosPorCargo(idCargo);
        CandidatoResponse candidato = candidatos.stream()
                .filter(item -> idCandidato.equals(item.idCandidato()))
                .findFirst()
                .orElseThrow(() -> httpException(Status.NOT_FOUND, "El candidato no pertenece al cargo indicado"));

        if (!Boolean.TRUE.equals(candidato.estado())) {
            throw httpException(Status.FORBIDDEN, "El candidato no esta activo");
        }
    }

    private void registrarAuditoriaSegura(String tipoEvento, String descripcion, String servicioOrigen) {
        try {
            auditClient.registrarEvento(new EventoAuditoriaDTO(tipoEvento, descripcion, servicioOrigen));
        } catch (Exception exception) {
            LOGGER.warnf(exception, "No fue posible registrar la auditoría %s", tipoEvento);
        }
    }

    private void registrarMetricaSegura(Runnable accion, String nombreMetrica) {
        try {
            accion.run();
        } catch (Exception exception) {
            LOGGER.warnf(exception, "No fue posible registrar la métrica %s", nombreMetrica);
        }
    }

    private WebApplicationException httpException(Status status, String message) {
        return new WebApplicationException(Response.status(status).entity(message).build());
    }
}