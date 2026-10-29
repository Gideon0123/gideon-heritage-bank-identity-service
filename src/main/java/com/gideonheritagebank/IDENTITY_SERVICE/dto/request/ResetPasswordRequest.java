package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class ResetPasswordRequest {

    @NotBlank(message = "Password-reset token is required")
    @Size(max = 512, message = "Password-reset token is too long")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(
            min = 12,
            max = 128,
            message = "New password must contain between 12 and 128 characters"
    )
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmNewPassword;

    @AssertTrue(message = "New password and confirmation password must match")
    public boolean isPasswordConfirmationMatching() {
        if (newPassword == null || confirmNewPassword == null) {
            return true;
        }

        return newPassword.equals(confirmNewPassword);
    }
}
