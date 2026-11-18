package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = "Verification token is required")
        @Size(max = 512, message = "Verification token is too long")
        @Schema(description = "Token. Sensitive credential/token value; never expose in responses.", accessMode = Schema.AccessMode.WRITE_ONLY)
        String token
) {
}
