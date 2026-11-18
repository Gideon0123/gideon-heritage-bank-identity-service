package com.gideonheritagebank.IDENTITY_SERVICE.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;

import java.time.Instant;

public record AuthenticationResult(
        @Schema(description = "Identity.")
        IdentityResponse identity,
        @Schema(description = "Access token. Sensitive credential/token value; never expose in responses.", accessMode = Schema.AccessMode.WRITE_ONLY)
        String accessToken,
        @Schema(description = "Refresh token. Sensitive credential/token value; never expose in responses.", accessMode = Schema.AccessMode.WRITE_ONLY)
        String refreshToken,
        @Schema(description = "Access token expires at.")
        Instant accessTokenExpiresAt,
        @Schema(description = "Refresh token expires at.")
        Instant refreshTokenExpiresAt
) {

    @Override
    public String toString() {
        return "AuthenticationResult[" +
                "identity=" + identity +
                ", accessToken=[REDACTED]" +
                ", refreshToken=[REDACTED]" +
                ", accessTokenExpiresAt=" + accessTokenExpiresAt +
                ", refreshTokenExpiresAt=" + refreshTokenExpiresAt +
                ']';
    }
}
