package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import java.time.Instant;

public record ProfileImageResponse(
        String objectKey,
        String imageUrl,
        Instant urlExpiresAt
) {
}