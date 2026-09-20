package sisve.ec.auth.service;

import sisve.ec.auth.client.AuditClient;
import sisve.ec.auth.db.SesionEntity;
import sisve.ec.auth.db.PollingStationMemberEntity;
import sisve.ec.auth.db.PollingStationMemberSessionEntity;
import sisve.ec.auth.db.VotanteEntity;
import sisve.ec.auth.dto.EventoAuditoriaDTO;
import sisve.ec.auth.dto.LoginRequest;
import sisve.ec.auth.dto.LoginResponse;
import sisve.ec.auth.dto.MemberLoginRequest;
import sisve.ec.auth.dto.MemberLoginResponse;
import sisve.ec.auth.dto.ValidateResponse;
import sisve.ec.auth.mapper.AuthMapper;
import sisve.ec.auth.repository.SesionRepository;
import sisve.ec.auth.repository.PollingStationMemberRepository;
import sisve.ec.auth.repository.PollingStationMemberSessionRepository;
import sisve.ec.auth.repository.VotanteRepository;
import sisve.ec.auth.security.JwtTokenGenerator;
import sisve.ec.auth.security.PasswordHasher;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;

@ApplicationScoped
public class AuthService {

    @Inject
    VotanteRepository votanteRepository;

    @Inject
    SesionRepository sesionRepository;

    @Inject
    PollingStationMemberRepository pollingStationMemberRepository;

    @Inject
    PollingStationMemberSessionRepository pollingStationMemberSessionRepository;

    @Inject
    JwtTokenGenerator jwtTokenGenerator;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    AuthMapper authMapper;

    @Inject
    @RestClient
    AuditClient auditClient;

    @Inject
    MeterRegistry meterRegistry;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        VotanteEntity votante = votanteRepository.findByCedulaAndCorreo(request.cedula(), request.correoInstitucional())
                .orElseGet(() -> {
                    registrarAuditoria("LOGIN_FALLIDO", "Credenciales inválidas para cédula " + request.cedula());
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                });

        if (!Boolean.TRUE.equals(votante.estado)) {
            registrarAuditoria("LOGIN_FALLIDO", "Votante inactivo: " + votante.cedula);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        String token = jwtTokenGenerator.generarToken(votante);
        String tokenHash = jwtTokenGenerator.calcularHash(token);
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(30);

        SesionEntity sesion = authMapper.toSesionEntity(votante.idVotante, tokenHash, expiracion);
        sesionRepository.persist(sesion);

        auditClient.registrarEvento(new EventoAuditoriaDTO(
                "LOGIN_EXITOSO",
                "Votante " + votante.cedula,
                "auth-service"
        ));

        meterRegistry.counter("auth.login.exitoso").increment();
        return authMapper.toLoginResponse(votante, token);
    }

    @Transactional
    public MemberLoginResponse memberLogin(MemberLoginRequest request) {
        PollingStationMemberEntity member = pollingStationMemberRepository
                .findByUserIdentifier(request.userIdentifier())
                .filter(item -> Boolean.TRUE.equals(item.status))
                .orElseThrow(() -> new WebApplicationException(Response.Status.UNAUTHORIZED));

        if (!passwordHasher.matches(request.password(), member.passwordSalt, member.passwordHash)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        String token = jwtTokenGenerator.generarToken(member);
        PollingStationMemberSessionEntity session = new PollingStationMemberSessionEntity();
        session.idMiembroMesa = member.idMiembroMesa;
        session.tokenHash = jwtTokenGenerator.calcularHash(token);
        session.fechaExpiracion = LocalDateTime.now().plusMinutes(30);
        pollingStationMemberSessionRepository.persist(session);

        return new MemberLoginResponse(token, member.idMiembroMesa, member.pollingStationId,
                member.userIdentifier, member.fullName, "POLLING_STATION_MEMBER");
    }

    @Transactional
    public void logout(String token) {
        String tokenHash = jwtTokenGenerator.calcularHash(token);

        var memberSession = pollingStationMemberSessionRepository.findByTokenHash(tokenHash);
        if (memberSession.isPresent()) {
            memberSession.get().estado = Boolean.FALSE;
            return;
        }

        SesionEntity sesion = sesionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new WebApplicationException(Response.Status.UNAUTHORIZED));

        sesionRepository.invalidarSesion(sesion.idSesion);
        registrarAuditoria("LOGOUT", "Sesión invalidada para votante " + sesion.idVotante);
        meterRegistry.counter("auth.logout.exitoso").increment();
    }

    public ValidateResponse validarToken(String token) {
        String tokenHash = jwtTokenGenerator.calcularHash(token);

        SesionEntity sesion = sesionRepository.findByTokenHash(tokenHash)
                .filter(this::validarSesion)
                .orElseThrow(() -> new WebApplicationException(Response.Status.UNAUTHORIZED));

        VotanteEntity votante = votanteRepository.findByIdOptional(sesion.idVotante)
                .orElseThrow(() -> new WebApplicationException(Response.Status.UNAUTHORIZED));

        return authMapper.toValidateResponse(votante);
    }

    private boolean validarSesion(SesionEntity sesion) {
        if (sesion.fechaExpiracion.isBefore(LocalDateTime.now())) {
            sesionRepository.invalidarSesion(sesion.idSesion);
            return false;
        }
        return true;
    }

    private void registrarAuditoria(String tipoEvento, String descripcion) {
        auditClient.registrarEvento(new EventoAuditoriaDTO(tipoEvento, descripcion, "auth-service"));
    }
}