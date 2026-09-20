package sisve.ec.auth.dto;

public record MemberLoginResponse(
        String token,
        Long memberId,
        Long pollingStationId,
        String userIdentifier,
        String fullName,
        String role
) {
}
