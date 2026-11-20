package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeRequest(

        @NotBlank(message = "The new email address is required")
        @Email(message = "A valid email address is required")
        @Size(max = 254, message = "Email address cannot exceed 254 characters")
        @Schema(
                description = "The new email address to associate with the identity.",
                example = "newaddress@example.com"
        )
        String newEmail

) {
}