package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendCustomerRequest(

        @NotBlank(message = "A suspension reason is required.")
        @Size(
                min = 5,
                max = 1000,
                message = "Suspension reason must contain between 5 and 1000 characters."
        )
        String reason

) {
}