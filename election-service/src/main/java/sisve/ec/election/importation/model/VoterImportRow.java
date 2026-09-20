package sisve.ec.election.importation.model;

public record VoterImportRow(int row, long ref, String cedula, String correoInstitucional,
                             String nombres, String apellidos, boolean estado, boolean voto,
                             long electionRef, long stationRef, String participationStatus) {
}
