package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TerminateStaffEmploymentRequest(

        @NotNull(message = "Employment end date is required")
        @Schema(description = "Employment end date.", example = "2026-11-18")
        LocalDate employmentEndDate,

        @NotBlank(message = "Termination reason is required")
        @Size(max = 500, message = "Termination reason cannot exceed 500 characters")
        @Schema(description = "Reason.", example = "Administrative review")
        String reason

) {
}