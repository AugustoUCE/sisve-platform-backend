package sisve.ec.pollingstation.dto;

import java.time.LocalDateTime;

public record PollingStationMemberResponse(
        Long idPollingStationMember,
        Long idPollingStation,
        String userIdentifier,
        String fullName,
        String institutionalEmail,
        String role,
        Boolean status,
        LocalDateTime createdAt
) {
}
