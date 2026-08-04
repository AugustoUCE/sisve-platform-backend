package sisve.ec.pollingstation.dto;

import jakarta.validation.constraints.NotBlank;

public record OpenStationRequest(
        @NotBlank String openedBy
) {
}
