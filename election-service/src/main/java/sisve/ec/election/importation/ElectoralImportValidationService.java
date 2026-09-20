package sisve.ec.election.importation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import sisve.ec.election.importation.dto.ValidationErrorDTO;
import sisve.ec.election.importation.dto.ValidationResponseDTO;
import sisve.ec.election.importation.dto.ValidationSummaryDTO;
import sisve.ec.election.importation.model.CandidateImportRow;
import sisve.ec.election.importation.model.ElectionImportRow;
import sisve.ec.election.importation.model.ElectoralWorkbook;
import sisve.ec.election.importation.model.PollingMemberImportRow;
import sisve.ec.election.importation.model.PollingStationImportRow;
import sisve.ec.election.importation.model.PositionImportRow;
import sisve.ec.election.importation.model.VoterImportRow;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ElectoralImportValidationService {
    private final XlsxElectoralReader reader;

    @Inject
    public ElectoralImportValidationService(XlsxElectoralReader reader) {
        this.reader = reader;
    }

    public ValidationResponseDTO validate(InputStream input) {
        XlsxElectoralReader.ReadResult read = reader.read(input);
        List<ValidationErrorDTO> errors = new ArrayList<>(read.errors());
        ElectoralWorkbook workbook = read.workbook();
        if (workbook != null) crossValidate(workbook, errors);
        ValidationSummaryDTO summary = workbook == null
                ? new ValidationSummaryDTO(0, 0, 0, 0, 0, 0)
                : new ValidationSummaryDTO(workbook.elections().size(), workbook.voters().size(), workbook.pollingStations().size(), workbook.members().size(), workbook.positions().size(), workbook.candidates().size());
        return new ValidationResponseDTO(errors.isEmpty(), summary, List.copyOf(errors), List.of());
    }

    private void crossValidate(ElectoralWorkbook workbook, List<ValidationErrorDTO> errors) {
        Map<Long, ElectionImportRow> elections = mapElections(workbook.elections());
        Map<Long, PollingStationImportRow> stations = mapStations(workbook.pollingStations());
        Map<Long, VoterImportRow> voters = mapVoters(workbook.voters());
        Map<Long, PositionImportRow> positions = mapPositions(workbook.positions());

        for (PollingStationImportRow station : workbook.pollingStations()) {
            if (!elections.containsKey(station.electionRef())) error(errors, "MESAS", station.row(), "id_eleccion_ref", "UNKNOWN_ELECTION_REF", "La eleccion referenciada no existe.");
        }
        for (VoterImportRow voter : workbook.voters()) {
            PollingStationImportRow station = stations.get(voter.stationRef());
            if (!elections.containsKey(voter.electionRef())) error(errors, "VOTANTES", voter.row(), "id_eleccion_ref", "UNKNOWN_ELECTION_REF", "La eleccion referenciada no existe.");
            if (station == null) error(errors, "VOTANTES", voter.row(), "id_mesa_ref", "UNKNOWN_STATION_REF", "La mesa referenciada no existe.");
            else if (station.electionRef() != voter.electionRef()) error(errors, "VOTANTES", voter.row(), "id_mesa_ref", "STATION_ELECTION_MISMATCH", "La mesa no pertenece a la eleccion del votante.");
        }
        for (PositionImportRow position : workbook.positions()) {
            if (!elections.containsKey(position.electionRef())) error(errors, "CARGOS", position.row(), "id_eleccion_ref", "UNKNOWN_ELECTION_REF", "La eleccion referenciada no existe.");
        }
        for (CandidateImportRow candidate : workbook.candidates()) {
            if (!positions.containsKey(candidate.positionRef())) error(errors, "CANDIDATOS", candidate.row(), "id_cargo_ref", "UNKNOWN_POSITION_REF", "El cargo referenciado no existe.");
        }
        for (PollingMemberImportRow member : workbook.members()) {
            if (!stations.containsKey(member.stationRef())) error(errors, "MIEMBROS_MESA", member.row(), "id_mesa_ref", "UNKNOWN_STATION_REF", "La mesa referenciada no existe.");
            if ("SI".equals(member.esVotante())) {
                VoterImportRow voter = member.voterRef() == null ? null : voters.get(member.voterRef());
                if (voter == null) error(errors, "MIEMBROS_MESA", member.row(), "id_votante_ref", "UNKNOWN_VOTER_REF", "El votante referenciado no existe.");
                else if (voter.stationRef() != member.stationRef()) error(errors, "MIEMBROS_MESA", member.row(), "id_votante_ref", "VOTER_STATION_MISMATCH", "El miembro-votante no pertenece a la misma mesa.");
            }
        }
    }

    private Map<Long, ElectionImportRow> mapElections(List<ElectionImportRow> rows) { Map<Long, ElectionImportRow> map = new HashMap<>(); rows.forEach(row -> map.putIfAbsent(row.ref(), row)); return map; }
    private Map<Long, PollingStationImportRow> mapStations(List<PollingStationImportRow> rows) { Map<Long, PollingStationImportRow> map = new HashMap<>(); rows.forEach(row -> map.putIfAbsent(row.ref(), row)); return map; }
    private Map<Long, VoterImportRow> mapVoters(List<VoterImportRow> rows) { Map<Long, VoterImportRow> map = new HashMap<>(); rows.forEach(row -> map.putIfAbsent(row.ref(), row)); return map; }
    private Map<Long, PositionImportRow> mapPositions(List<PositionImportRow> rows) { Map<Long, PositionImportRow> map = new HashMap<>(); rows.forEach(row -> map.putIfAbsent(row.ref(), row)); return map; }
    private void error(List<ValidationErrorDTO> errors, String sheet, int row, String field, String code, String message) { errors.add(new ValidationErrorDTO(sheet, row, field, code, message)); }
}
