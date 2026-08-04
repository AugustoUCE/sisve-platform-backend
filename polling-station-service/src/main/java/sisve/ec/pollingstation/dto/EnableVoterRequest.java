package sisve.ec.pollingstation.dto;

import jakarta.validation.constraints.NotBlank;

public record EnableVoterRequest(
        @NotBlank String enabledBy
) {
}
