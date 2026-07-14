package sisve.ec.election.dto;

import jakarta.validation.constraints.NotBlank;

public record CargoRequest(@NotBlank String nombre) {
}