package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteStaffOnboardingRequest {

    @NotBlank(message = "Onboarding token is required")
    @Schema(description = "Token. Sensitive credential/token value; never expose in responses.", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String token;

    @NotBlank(message = "New password is required")
    @Size(
            min = 12,
            max = 128,
            message = "Password must contain between 12 and 128 characters"
    )
    @Schema(description = "New password. Sensitive credential/token value; never expose in responses.", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @Size(
            min = 12,
            max = 128,
            message = "Password confirmation must contain between 12 and 128 characters"
    )
    @Schema(description = "Confirm password. Sensitive credential/token value; never expose in responses.", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String confirmPassword;
}