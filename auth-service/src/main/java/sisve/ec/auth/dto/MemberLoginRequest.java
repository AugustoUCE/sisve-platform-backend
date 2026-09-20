package sisve.ec.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberLoginRequest(
        @NotBlank String userIdentifier,
        @NotBlank String password
) {
}
