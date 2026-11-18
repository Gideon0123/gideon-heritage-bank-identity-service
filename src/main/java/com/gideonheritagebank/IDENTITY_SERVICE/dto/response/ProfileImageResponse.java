package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record ProfileImageResponse(
        @Schema(description = "Object key.")
        String objectKey,
        @Schema(description = "Image url.")
        String imageUrl,
        @Schema(description = "Url expires at.")
        Instant urlExpiresAt
) {
}