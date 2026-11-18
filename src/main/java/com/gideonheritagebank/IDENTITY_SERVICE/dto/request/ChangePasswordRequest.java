package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        @Size(max = 128, message = "Current password must not exceed 128 characters")
        @Schema(description = "Current password. This value is sensitive and is write-only.", accessMode = Schema.AccessMode.WRITE_ONLY)
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(
                min = 12,
                max = 128,
                message = "New password must contain between 12 and 128 characters"
        )
        @Schema(description = "New password. This value is sensitive and is write-only.", accessMode = Schema.AccessMode.WRITE_ONLY)
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(
                max = 128,
                message = "Password confirmation must not exceed 128 characters"
        )
        @Schema(description = "Confirm password. Sensitive credential/token value; never expose in responses.", accessMode = Schema.AccessMode.WRITE_ONLY)
        String confirmPassword

) {
}