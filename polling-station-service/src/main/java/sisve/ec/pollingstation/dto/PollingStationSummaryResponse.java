package sisve.ec.pollingstation.dto;

public record PollingStationSummaryResponse(
        Long idPollingStation,
        Long idElection,
        Long total,
        Long pending,
        Long enabled,
        Long voted,
        Long blocked,
        String status
) {
}
