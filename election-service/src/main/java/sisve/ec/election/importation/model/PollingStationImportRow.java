package sisve.ec.election.importation.model;

public record PollingStationImportRow(int row, long ref, long electionRef, String code,
                                      String name, String location, String status) {
}
