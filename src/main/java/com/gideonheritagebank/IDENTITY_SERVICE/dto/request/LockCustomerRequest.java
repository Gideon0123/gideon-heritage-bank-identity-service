package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record LockCustomerRequest(

        /*
         * Null means an indefinite lock.
         * A non-null value must be in the future.
         */
        @Schema(description = "Locked until.")
        Instant lockedUntil,

        @NotBlank(message = "A reason is required.")
        @Size(
                min = 5,
                max = 1000,
                message = "Reason must contain between 5 and 1000 characters."
        )
        @Schema(description = "Reason.", example = "Administrative review")
        String reason

) {
}
