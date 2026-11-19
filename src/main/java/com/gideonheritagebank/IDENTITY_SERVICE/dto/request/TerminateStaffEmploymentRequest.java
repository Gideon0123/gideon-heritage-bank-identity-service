package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TerminateStaffEmploymentRequest(

        @NotNull(message = "Employment end date is required")
        LocalDate employmentEndDate,

        @NotBlank(message = "Termination reason is required")
        @Size(max = 500, message = "Termination reason cannot exceed 500 characters")
        String reason

) {
}