package sisve.ec.pollingstation.dto;

import java.time.LocalDateTime;

public record ElectoralRollResponse(
        Long idElectoralRoll,
        Long idPollingStation,
        Long idElection,
        Long idVoter,
        String cedula,
        String fullName,
        String institutionalEmail,
        String participationStatus,
        LocalDateTime enabledAt,
        String enabledBy,
        LocalDateTime votedAt,
        LocalDateTime blockedAt,
        String blockedBy,
        String blockReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
