package sisve.ec.election.importation.model;

public record CandidateImportRow(int row, long ref, long positionRef, String nombres,
                                 String apellidos, String lista, boolean estado) {
}
