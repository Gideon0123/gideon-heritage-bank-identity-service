package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerClosureRequest(

        @NotBlank(message = "Your current password is required.")
        @Schema(description = "Current password. This value is sensitive and is write-only.", accessMode = Schema.AccessMode.WRITE_ONLY)
        String currentPassword,

        @NotBlank(message = "A closure reason is required.")
        @Size(
                min = 10,
                max = 1000,
                message = "Closure reason must contain between 10 and 1000 characters."
        )
        @Schema(description = "Reason.", example = "Administrative review")
        String reason,

        @AssertTrue(message = "You must explicitly confirm the closure request.")
        @Schema(description = "Confirm closure.")
        boolean confirmClosure

) {
}
