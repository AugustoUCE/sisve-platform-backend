package sisve.ec.election.importation.dto;

public record ValidationErrorDTO(String sheet, int row, String field, String code, String message) {
}
