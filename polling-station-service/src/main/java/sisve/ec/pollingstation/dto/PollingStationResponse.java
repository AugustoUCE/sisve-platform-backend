package sisve.ec.pollingstation.dto;

import java.time.LocalDateTime;

public record PollingStationResponse(
        Long idPollingStation,
        Long idElection,
        String code,
        String name,
        String location,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
