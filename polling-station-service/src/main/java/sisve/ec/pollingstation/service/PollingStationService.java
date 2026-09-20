package sisve.ec.pollingstation.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import sisve.ec.pollingstation.client.AuditClient;
import sisve.ec.pollingstation.db.ElectoralRollEntity;
import sisve.ec.pollingstation.db.ParticipationStatus;
import sisve.ec.pollingstation.db.PollingStationEntity;
import sisve.ec.pollingstation.db.PollingStationStatus;
import sisve.ec.pollingstation.dto.BlockVoterRequest;
import sisve.ec.pollingstation.dto.CloseStationRequest;
import sisve.ec.pollingstation.dto.EnableVoterRequest;
import sisve.ec.pollingstation.dto.ElectoralRollResponse;
import sisve.ec.pollingstation.dto.EventoAuditoriaDTO;
import sisve.ec.pollingstation.dto.OpenStationRequest;
import sisve.ec.pollingstation.dto.PollingStationEligibilityResponse;
import sisve.ec.pollingstation.dto.PollingStationMemberResponse;
import sisve.ec.pollingstation.dto.PollingStationResponse;
import sisve.ec.pollingstation.dto.PollingStationSummaryResponse;
import sisve.ec.pollingstation.mapper.PollingStationMapper;
import sisve.ec.pollingstation.repository.ElectoralRollRepository;
import sisve.ec.pollingstation.repository.PollingStationMemberRepository;
import sisve.ec.pollingstation.repository.PollingStationRepository;
import sisve.ec.pollingstation.repository.VoteAttemptRepository;
import sisve.ec.pollingstation.db.VoteAttemptEntity;
import sisve.ec.pollingstation.dto.VoteReservationResponse;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PollingStationService {

    private static final Logger LOGGER = Logger.getLogger(PollingStationService.class);

    @Inject
    PollingStationRepository pollingStationRepository;

    @Inject
    PollingStationMemberRepository pollingStationMemberRepository;

    @Inject
    ElectoralRollRepository electoralRollRepository;

    @Inject
    VoteAttemptRepository voteAttemptRepository;

    @Inject
    PollingStationMapper pollingStationMapper;

    @Inject
    @RestClient
    AuditClient auditClient;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    JsonWebToken jwt;

    public List<PollingStationResponse> listarPorEleccion(Long idElection) {
        requireRole("POLLING_STATION_MEMBER");
        Long stationId = claimLong("pollingStationId");
        return pollingStationRepository.findByIdOptional(stationId)
            .filter(station -> idElection.equals(station.idElection))
            .map(station -> List.of(pollingStationMapper.toPollingStationResponse(station)))
            .orElseGet(List::of);
    }

    public PollingStationResponse obtenerPorId(Long idPollingStation) {
        requireMemberStation(idPollingStation);
        return pollingStationMapper.toPollingStationResponse(obtenerMesa(idPollingStation));
    }

    public List<ElectoralRollResponse> obtenerPadron(Long idPollingStation) {
        requireMemberStation(idPollingStation);
        obtenerMesa(idPollingStation);
        return pollingStationMapper.toElectoralRollResponseList(electoralRollRepository.findByPollingStation(idPollingStation));
    }

    public List<ElectoralRollResponse> obtenerPadronDeMiMesa(String search) {
        requireRole("POLLING_STATION_MEMBER");
        Long stationId = claimLong("pollingStationId");
        List<ElectoralRollEntity> roll = search == null || search.isBlank()
                ? electoralRollRepository.findByPollingStation(stationId)
                : electoralRollRepository.searchByPollingStation(stationId, search.trim());
        return pollingStationMapper.toElectoralRollResponseList(roll);
    }

    @Transactional
    public ElectoralRollResponse habilitarVotanteDeMiMesa(Long idVoter) {
        requireRole("POLLING_STATION_MEMBER");
        Long stationId = claimLong("pollingStationId");
        Long memberId = claimLong("memberId");
        return habilitarVotante(stationId, idVoter, new EnableVoterRequest(String.valueOf(memberId)));
    }

    public ElectoralRollResponse buscarVotantePorCedula(Long idPollingStation, String cedula) {
        requireMemberStation(idPollingStation);
        obtenerMesa(idPollingStation);
        ElectoralRollEntity roll = electoralRollRepository.findByPollingStationAndCedula(idPollingStation, cedula)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        return pollingStationMapper.toElectoralRollResponse(roll);
    }

    public List<PollingStationMemberResponse> listarMiembros(Long idPollingStation) {
        requireMemberStation(idPollingStation);
        obtenerMesa(idPollingStation);
        return pollingStationMapper.toPollingStationMemberResponseList(
                pollingStationMemberRepository.findByPollingStation(idPollingStation)
        );
    }

    @Transactional
    public PollingStationResponse abrirMesa(Long idPollingStation, OpenStationRequest request) {
        requireMemberStation(idPollingStation);
        PollingStationEntity station = obtenerMesa(idPollingStation);
        station.status = PollingStationStatus.OPEN;
        station.openedAt = LocalDateTime.now();
        station.updatedAt = LocalDateTime.now();
        registrarAuditoriaSegura("MESA_ABIERTA", "Mesa " + station.code + " abierta por " + request.openedBy(), null, "polling-station-service");
        registrarMetricaSegura(() -> meterRegistry.counter("polling_station_mesas_abiertas_total").increment(), "polling_station_mesas_abiertas_total");
        return pollingStationMapper.toPollingStationResponse(station);
    }

    @Transactional
    public PollingStationResponse cerrarMesa(Long idPollingStation, CloseStationRequest request) {
        requireMemberStation(idPollingStation);
        PollingStationEntity station = obtenerMesa(idPollingStation);
        if (!PollingStationStatus.OPEN.equalsIgnoreCase(station.status)) {
            registrarAuditoriaSegura("ERROR_CIERRE_MESA", "La mesa " + station.code + " no está abierta", null, "polling-station-service");
            throw httpException(Status.CONFLICT, "La mesa no se encuentra abierta");
        }

        station.status = PollingStationStatus.CLOSED;
        station.closedAt = LocalDateTime.now();
        station.updatedAt = LocalDateTime.now();
        registrarAuditoriaSegura("MESA_CERRADA", "Mesa " + station.code + " cerrada por " + request.closedBy(), null, "polling-station-service");
        registrarMetricaSegura(() -> meterRegistry.counter("polling_station_mesas_cerradas_total").increment(), "polling_station_mesas_cerradas_total");
        return pollingStationMapper.toPollingStationResponse(station);
    }

    @Transactional
    public ElectoralRollResponse habilitarVotante(Long idPollingStation, Long idVoter, EnableVoterRequest request) {
        requireMemberStation(idPollingStation);
        PollingStationEntity station = obtenerMesa(idPollingStation);
        validarMesaAbierta(station, "ERROR_HABILITACION_VOTANTE", "La mesa no se encuentra abierta");

        ElectoralRollEntity roll = obtenerRegistroPorMesaYVotante(idPollingStation, idVoter);
        if (ParticipationStatus.VOTED.equalsIgnoreCase(roll.participationStatus)) {
            throw httpException(Status.CONFLICT, "El votante ya registró su voto en esta mesa");
        }
        if (!ParticipationStatus.PENDING.equalsIgnoreCase(roll.participationStatus)) {
            throw httpException(Status.CONFLICT, "El votante no puede ser habilitado desde su estado actual");
        }

        LocalDateTime enabledAt = LocalDateTime.now();
        long updated = electoralRollRepository.enablePending(idPollingStation, idVoter, request.enabledBy(), enabledAt);
        if (updated != 1) {
            throw httpException(Status.CONFLICT, "El votante ya fue habilitado o cambió de estado");
        }
        roll = obtenerRegistroPorMesaYVotante(idPollingStation, idVoter);

        registrarAuditoriaSegura("VOTANTE_HABILITADO", "Votante " + roll.cedula + " habilitado por " + request.enabledBy(), null, "polling-station-service");
        registrarMetricaSegura(() -> meterRegistry.counter("polling_station_votantes_habilitados_total").increment(), "polling_station_votantes_habilitados_total");
        return pollingStationMapper.toElectoralRollResponse(roll);
    }

    @Transactional
    public ElectoralRollResponse bloquearVotante(Long idPollingStation, Long idVoter, BlockVoterRequest request) {
        requireMemberStation(idPollingStation);
        PollingStationEntity station = obtenerMesa(idPollingStation);
        validarMesaAbierta(station, "ERROR_HABILITACION_VOTANTE", "La mesa no se encuentra abierta");

        ElectoralRollEntity roll = obtenerRegistroPorMesaYVotante(idPollingStation, idVoter);
        if (ParticipationStatus.VOTED.equalsIgnoreCase(roll.participationStatus)) {
            throw httpException(Status.CONFLICT, "El votante ya registró su voto en esta mesa");
        }

        roll.participationStatus = ParticipationStatus.BLOCKED;
        roll.blockedAt = LocalDateTime.now();
        roll.blockedBy = request.blockedBy();
        roll.blockReason = request.reason();
        roll.updatedAt = LocalDateTime.now();

        registrarAuditoriaSegura("VOTANTE_BLOQUEADO", "Votante " + roll.cedula + " bloqueado por " + request.blockedBy(), null, "polling-station-service");
        registrarMetricaSegura(() -> meterRegistry.counter("polling_station_votantes_bloqueados_total").increment(), "polling_station_votantes_bloqueados_total");
        return pollingStationMapper.toElectoralRollResponse(roll);
    }

    @Transactional
    public ElectoralRollResponse marcarVotanteComoVotado(Long idElection, Long idVoter) {
        requireVoter(idVoter);
        ElectoralRollEntity roll = electoralRollRepository.findByElectionAndVoter(idElection, idVoter)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

        if (!ParticipationStatus.ENABLED.equalsIgnoreCase(roll.participationStatus)) {
            throw httpException(Status.FORBIDDEN, "El votante no está habilitado por la mesa electoral");
        }

        roll.participationStatus = ParticipationStatus.VOTED;
        roll.votedAt = LocalDateTime.now();
        roll.updatedAt = LocalDateTime.now();

        registrarAuditoriaSegura("VOTANTE_MARCADO_VOTADO", "Votante " + roll.cedula + " marcado como votado", null, "polling-station-service");
        registrarMetricaSegura(() -> meterRegistry.counter("polling_station_votantes_marcados_votado_total").increment(), "polling_station_votantes_marcados_votado_total");
        return pollingStationMapper.toElectoralRollResponse(roll);
    }

    @Transactional
    public ElectoralRollResponse marcarMiVotacionComoVotada(Long idElection) {
        requireRole("VOTER");
        return marcarVotanteComoVotado(idElection, claimLong("idVotante"));
    }

    public PollingStationEligibilityResponse validarElegibilidad(Long idElection, Long idVoter) {
        requireVoter(idVoter);
        ElectoralRollEntity roll = electoralRollRepository.findByElectionAndVoter(idElection, idVoter)
                .orElse(null);
        if (roll == null) {
            return pollingStationMapper.toEligibilityResponse(idElection, idVoter, null, false, ParticipationStatus.PENDING, PollingStationStatus.CLOSED);
        }

        PollingStationEntity station = obtenerMesa(roll.idPollingStation);
        boolean eligible = PollingStationStatus.OPEN.equalsIgnoreCase(station.status)
                && ParticipationStatus.ENABLED.equalsIgnoreCase(roll.participationStatus);
        return pollingStationMapper.toEligibilityResponse(roll, station, eligible);
    }

    public PollingStationEligibilityResponse validarMiElegibilidad(Long idElection) {
        requireRole("VOTER");
        return validarElegibilidad(idElection, claimLong("idVotante"));
    }

    @Transactional
    public VoteReservationResponse reservarVoto(Long idElection) {
        requireRole("VOTER");
        Long idVoter = claimLong("idVotante");
        PollingStationEligibilityResponse eligibility = validarElegibilidad(idElection, idVoter);
        if (!Boolean.TRUE.equals(eligibility.eligible())) {
            throw httpException(Status.FORBIDDEN, "El votante no está habilitado para votar");
        }
        if (voteAttemptRepository.findByElectionAndVoter(idElection, idVoter).isPresent()) {
            throw httpException(Status.CONFLICT, "El votante ya inició o completó su votación");
        }
        VoteAttemptEntity attempt = new VoteAttemptEntity();
        attempt.idElection = idElection;
        attempt.idVoter = idVoter;
        attempt.status = "RESERVED";
        attempt.createdAt = LocalDateTime.now();
        try {
            voteAttemptRepository.persistAndFlush(attempt);
        } catch (Exception exception) {
            throw httpException(Status.CONFLICT, "El votante ya inició su votación");
        }
        return new VoteReservationResponse("RESERVED");
    }

    @Transactional
    public VoteReservationResponse completarVoto(Long idElection) {
        requireRole("VOTER");
        Long idVoter = claimLong("idVotante");
        VoteAttemptEntity attempt = voteAttemptRepository.findByElectionAndVoter(idElection, idVoter)
                .orElseThrow(() -> httpException(Status.CONFLICT, "No existe una reserva de voto"));
        attempt.status = "COMPLETED";
        return new VoteReservationResponse("COMPLETED");
    }

    @Transactional
    public void liberarReserva(Long idElection) {
        requireRole("VOTER");
        Long idVoter = claimLong("idVotante");
        voteAttemptRepository.delete("idElection = ?1 and idVoter = ?2 and status = 'RESERVED'", idElection, idVoter);
    }

    public PollingStationSummaryResponse resumen(Long idPollingStation) {
        requireMemberStation(idPollingStation);
        PollingStationEntity station = obtenerMesa(idPollingStation);
        long total = electoralRollRepository.countByPollingStation(idPollingStation);
        long pending = electoralRollRepository.countByPollingStationAndStatus(idPollingStation, ParticipationStatus.PENDING);
        long enabled = electoralRollRepository.countByPollingStationAndStatus(idPollingStation, ParticipationStatus.ENABLED);
        long voted = electoralRollRepository.countByPollingStationAndStatus(idPollingStation, ParticipationStatus.VOTED);
        long blocked = electoralRollRepository.countByPollingStationAndStatus(idPollingStation, ParticipationStatus.BLOCKED);
        return pollingStationMapper.toSummaryResponse(station, total, pending, enabled, voted, blocked);
    }

    private PollingStationEntity obtenerMesa(Long idPollingStation) {
        return pollingStationRepository.findByIdOptional(idPollingStation)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    private ElectoralRollEntity obtenerRegistroPorMesaYVotante(Long idPollingStation, Long idVoter) {
        return electoralRollRepository.findByPollingStationAndVoter(idPollingStation, idVoter)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    private void validarMesaAbierta(PollingStationEntity station, String tipoEventoError, String descripcionError) {
        if (!PollingStationStatus.OPEN.equalsIgnoreCase(station.status)) {
            registrarAuditoriaSegura(tipoEventoError, descripcionError, null, "polling-station-service");
            throw httpException(Status.CONFLICT, descripcionError);
        }
    }

    private void registrarAuditoriaSegura(String tipoEvento, String descripcion, String ipOrigen, String servicioOrigen) {
        try {
            auditClient.registrarEvento(new EventoAuditoriaDTO(tipoEvento, descripcion, ipOrigen, servicioOrigen));
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

    private void requireRole(String expectedRole) {
        if (jwt == null || !expectedRole.equals(jwt.getClaim("role"))) {
            throw httpException(Status.FORBIDDEN, "El rol no está autorizado para esta operación");
        }
    }

    private void requireMemberStation(Long idPollingStation) {
        requireRole("POLLING_STATION_MEMBER");
        if (!idPollingStation.equals(claimLong("pollingStationId"))) {
            throw httpException(Status.FORBIDDEN, "El miembro no pertenece a la mesa solicitada");
        }
        String userIdentifier = jwt.getSubject();
        pollingStationMemberRepository.findActiveByUserIdentifier(userIdentifier)
                .filter(member -> idPollingStation.equals(member.idPollingStation))
                .orElseThrow(() -> httpException(Status.FORBIDDEN, "El miembro no está asignado a esta mesa"));
    }

    private void requireVoter(Long idVoter) {
        requireRole("VOTER");
        if (!idVoter.equals(claimLong("idVotante"))) {
            throw httpException(Status.FORBIDDEN, "El votante autenticado no coincide con la solicitud");
        }
    }

    private Long claimLong(String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (claim instanceof Number number) return number.longValue();
        try {
            return Long.valueOf(String.valueOf(claim));
        } catch (Exception exception) {
            throw httpException(Status.FORBIDDEN, "El token no contiene una identidad válida");
        }
    }
}
