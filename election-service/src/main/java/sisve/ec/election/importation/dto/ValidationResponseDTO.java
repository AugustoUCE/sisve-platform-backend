package sisve.ec.election.importation.dto;

import java.util.List;

public record ValidationResponseDTO(boolean valid, ValidationSummaryDTO summary,
                                    List<ValidationErrorDTO> errors, List<String> warnings) {
}
