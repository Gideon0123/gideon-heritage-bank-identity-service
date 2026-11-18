package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DeactivateStaffRequest(

        @NotNull(message = "Effective date is required")
        @PastOrPresent(message = "Effective date cannot be in the future")
        @Schema(description = "Effective date.")
        LocalDate effectiveDate,

        @NotBlank(message = "Deactivation reason is required")
        @Size(max = 500, message = "Deactivation reason cannot exceed 500 characters")
        @Schema(description = "Reason.", example = "Administrative review")
        String reason

) {
}