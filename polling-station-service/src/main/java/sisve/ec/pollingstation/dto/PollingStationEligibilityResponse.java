package sisve.ec.pollingstation.dto;

public record PollingStationEligibilityResponse(
        Long idElection,
        Long idVoter,
        Long idPollingStation,
        Boolean eligible,
        String participationStatus,
        String pollingStationStatus
) {
}
