package sisve.ec.election.dto;

public record CargoResponse(
        Long idCargo,
        Long idEleccion,
        String nombre
) {
}