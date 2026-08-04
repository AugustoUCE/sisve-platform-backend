package sisve.ec.pollingstation.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockVoterRequest(
        @NotBlank String blockedBy,
        @NotBlank String reason
) {
}
