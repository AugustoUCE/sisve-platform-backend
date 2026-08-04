package sisve.ec.pollingstation.dto;

import jakarta.validation.constraints.NotBlank;

public record CloseStationRequest(
        @NotBlank String closedBy
) {
}
