package sisve.ec.election.importation.model;

public record PollingMemberImportRow(int row, long ref, String userIdentifier, String fullName,
                                     String institutionalEmail, long stationRef, String role,
                                     boolean status, String esVotante, Long voterRef) {
}
