package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        @Size(max = 128, message = "Current password must not exceed 128 characters")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(
                min = 12,
                max = 128,
                message = "New password must contain between 12 and 128 characters"
        )
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(
                max = 128,
                message = "Password confirmation must not exceed 128 characters"
        )
        String confirmPassword

) {
}