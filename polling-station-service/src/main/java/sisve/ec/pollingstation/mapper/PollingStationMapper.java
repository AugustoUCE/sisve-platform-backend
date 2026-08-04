package sisve.ec.pollingstation.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import sisve.ec.pollingstation.db.ElectoralRollEntity;
import sisve.ec.pollingstation.db.PollingStationEntity;
import sisve.ec.pollingstation.dto.ElectoralRollResponse;
import sisve.ec.pollingstation.dto.PollingStationEligibilityResponse;
import sisve.ec.pollingstation.dto.PollingStationMemberResponse;
import sisve.ec.pollingstation.dto.PollingStationResponse;
import sisve.ec.pollingstation.dto.PollingStationSummaryResponse;
import sisve.ec.pollingstation.db.PollingStationMemberEntity;

import java.util.List;

@ApplicationScoped
public class PollingStationMapper {

    public PollingStationResponse toPollingStationResponse(PollingStationEntity entity) {
        return new PollingStationResponse(
                entity.idPollingStation,
                entity.idElection,
                entity.code,
                entity.name,
                entity.location,
                entity.status,
                entity.openedAt,
                entity.closedAt,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public List<PollingStationResponse> toPollingStationResponseList(List<PollingStationEntity> entities) {
        return entities.stream().map(this::toPollingStationResponse).toList();
    }

    public PollingStationMemberResponse toPollingStationMemberResponse(PollingStationMemberEntity entity) {
        return new PollingStationMemberResponse(
                entity.idPollingStationMember,
                entity.idPollingStation,
                entity.userIdentifier,
                entity.fullName,
                entity.institutionalEmail,
                entity.role,
                entity.status,
                entity.createdAt
        );
    }

    public List<PollingStationMemberResponse> toPollingStationMemberResponseList(List<PollingStationMemberEntity> entities) {
        return entities.stream().map(this::toPollingStationMemberResponse).toList();
    }

    public ElectoralRollResponse toElectoralRollResponse(ElectoralRollEntity entity) {
        return new ElectoralRollResponse(
                entity.idElectoralRoll,
                entity.idPollingStation,
                entity.idElection,
                entity.idVoter,
                entity.cedula,
                entity.fullName,
                entity.institutionalEmail,
                entity.participationStatus,
                entity.enabledAt,
                entity.enabledBy,
                entity.votedAt,
                entity.blockedAt,
                entity.blockedBy,
                entity.blockReason,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public List<ElectoralRollResponse> toElectoralRollResponseList(List<ElectoralRollEntity> entities) {
        return entities.stream().map(this::toElectoralRollResponse).toList();
    }

    public PollingStationEligibilityResponse toEligibilityResponse(
            Long idElection,
            Long idVoter,
            Long idPollingStation,
            boolean eligible,
            String participationStatus,
            String pollingStationStatus
    ) {
        return new PollingStationEligibilityResponse(
                idElection,
                idVoter,
                idPollingStation,
                eligible,
                participationStatus,
                pollingStationStatus
        );
    }

    public PollingStationEligibilityResponse toEligibilityResponse(
            ElectoralRollEntity roll,
            PollingStationEntity station,
            boolean eligible
    ) {
        return toEligibilityResponse(
                roll.idElection,
                roll.idVoter,
                station.idPollingStation,
                eligible,
                roll.participationStatus,
                station.status
        );
    }

    public PollingStationSummaryResponse toSummaryResponse(
            PollingStationEntity station,
            long total,
            long pending,
            long enabled,
            long voted,
            long blocked
    ) {
        return new PollingStationSummaryResponse(
                station.idPollingStation,
                station.idElection,
                total,
                pending,
                enabled,
                voted,
                blocked,
                station.status
        );
    }
}
