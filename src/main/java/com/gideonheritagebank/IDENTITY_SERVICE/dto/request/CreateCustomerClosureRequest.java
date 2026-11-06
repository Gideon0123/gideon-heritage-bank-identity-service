package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerClosureRequest(

        @NotBlank(message = "Your current password is required.")
        String currentPassword,

        @NotBlank(message = "A closure reason is required.")
        @Size(
                min = 10,
                max = 1000,
                message = "Closure reason must contain between 10 and 1000 characters."
        )
        String reason,

        @AssertTrue(message = "You must explicitly confirm the closure request.")
        boolean confirmClosure

) {
}
