package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailChangeRequest(

        @NotBlank(message = "Email change verification token is required")
        @Size(max = 512, message = "Verification token is too long")
        @Schema(
                description = "One-time email change verification token.",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String token

) {
}
