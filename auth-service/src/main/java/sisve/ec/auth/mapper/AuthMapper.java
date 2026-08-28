package sisve.ec.auth.mapper;

import sisve.ec.auth.db.SesionEntity;
import sisve.ec.auth.db.VotanteEntity;
import sisve.ec.auth.dto.LoginResponse;
import sisve.ec.auth.dto.ValidateResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;

@ApplicationScoped
public class AuthMapper {

    public SesionEntity toSesionEntity(Long idVotante, String tokenHash, LocalDateTime expiracion) {
        SesionEntity sesion = new SesionEntity();
        sesion.idVotante = idVotante;
        sesion.tokenHash = tokenHash;
        sesion.fechaCreacion = LocalDateTime.now();
        sesion.fechaExpiracion = expiracion;
        sesion.estado = Boolean.TRUE;
        return sesion;
    }

    public LoginResponse toLoginResponse(VotanteEntity votante, String token) {
        return new LoginResponse(token, votante.idVotante, votante.correoInstitucional, votante.nombres, votante.apellidos, votante.voto);
    }

    public ValidateResponse toValidateResponse(VotanteEntity votante) {
        return new ValidateResponse(
                true,
                votante.idVotante,
                votante.cedula,
                votante.correoInstitucional,
                votante.nombres,
                votante.apellidos,
                votante.voto
        );
    }
}