package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCustomerClosureRequest(

        @NotBlank(message = "A rejection reason is required.")
        @Size(
                min = 10,
                max = 1000,
                message = "Rejection reason must contain between 10 and 1000 characters."
        )
        @Schema(description = "Reason.", example = "Administrative review")
        String reason

) {
}
