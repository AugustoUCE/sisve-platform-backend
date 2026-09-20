package sisve.ec.election.importation.model;

import java.util.List;

public record ElectoralWorkbook(
        List<ElectionImportRow> elections,
        List<PollingStationImportRow> pollingStations,
        List<VoterImportRow> voters,
        List<PollingMemberImportRow> members,
        List<PositionImportRow> positions,
        List<CandidateImportRow> candidates
) {
}
