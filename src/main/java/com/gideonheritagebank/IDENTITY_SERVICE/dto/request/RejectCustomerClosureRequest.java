package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCustomerClosureRequest(

        @NotBlank(message = "A rejection reason is required.")
        @Size(
                min = 10,
                max = 1000,
                message = "Rejection reason must contain between 10 and 1000 characters."
        )
        String reason

) {
}
