package com.gideonheritagebank.IDENTITY_SERVICE.dto.internal;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;

import java.time.Instant;

public record AuthenticationResult(
        IdentityResponse identity,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {
}
