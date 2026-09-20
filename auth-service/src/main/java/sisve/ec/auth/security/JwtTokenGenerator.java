package sisve.ec.auth.security;

import sisve.ec.auth.db.VotanteEntity;
import sisve.ec.auth.db.PollingStationMemberEntity;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@ApplicationScoped
public class JwtTokenGenerator {

    public String generarToken(VotanteEntity votante) {
        Instant expiracion = Instant.now().plus(30, ChronoUnit.MINUTES);
        return Jwt.issuer("sisve-auth")
                .subject(votante.cedula)
                .claim("idVotante", votante.idVotante)
                .claim("role", "VOTER")
                .upn(votante.correoInstitucional)
                .expiresAt(expiracion)
                .sign();
    }

    public String generarToken(PollingStationMemberEntity member) {
        Instant expiracion = Instant.now().plus(30, ChronoUnit.MINUTES);
        return Jwt.issuer("sisve-auth")
                .subject(member.userIdentifier)
                .claim("role", "POLLING_STATION_MEMBER")
                .claim("memberId", member.idMiembroMesa)
                .claim("pollingStationId", member.pollingStationId)
                .upn(member.institutionalEmail)
                .expiresAt(expiracion)
                .sign();
    }

    public String calcularHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo calcular el hash del token", exception);
        }
    }
}