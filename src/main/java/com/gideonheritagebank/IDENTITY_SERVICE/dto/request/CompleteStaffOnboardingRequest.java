package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

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
    private String token;

    @NotBlank(message = "New password is required")
    @Size(
            min = 12,
            max = 128,
            message = "Password must contain between 12 and 128 characters"
    )
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @Size(
            min = 12,
            max = 128,
            message = "Password confirmation must contain between 12 and 128 characters"
    )
    private String confirmPassword;
}