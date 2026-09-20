package sisve.ec.election.importation.model;

import java.time.LocalDateTime;

public record ElectionImportRow(int row, long ref, String nombre, String descripcion,
                                LocalDateTime fechaInicio, LocalDateTime fechaFin, String estado) {
}
