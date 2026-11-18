package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(

        @NotBlank(message = "Email address is required")
        @Email(message = "A valid email address is required")
        @Size(max = 254, message = "Email address is too long")
        @Schema(description = "Email.", example = "customer@example.com")
        String email

) {
}
