package sisve.ec.election.importation.dto;

public record ValidationSummaryDTO(int elections, int voters, int pollingStations,
                                   int members, int positions, int candidates) {
}
