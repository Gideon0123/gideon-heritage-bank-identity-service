package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateStaffEmploymentRequest(

        @Schema(description = "Department id.")
        UUID departmentId,

        @Schema(description = "Branch id.")
        UUID branchId,

        @Size(max = 120, message = "Job title cannot exceed 120 characters")
        @Schema(description = "Job title.", example = "Branch Operations Officer")
        String jobTitle,

        @Schema(description = "Employment type.")
        EmploymentType employmentType,

        @NotNull(message = "Employment status is required")
        @Schema(description = "Employment status.")
        EmploymentStatus employmentStatus,

        @NotNull(message = "Effective date is required")
        @Schema(description = "Effective from.", example = "2026-01-01")
        LocalDate effectiveFrom,

        @Schema(description = "Employment end date.", example = "2026-11-18")
        LocalDate employmentEndDate,

        @Schema(description = "Reporting manager id.")
        UUID reportingManagerId,

        @Size(max = 500, message = "Change reason cannot exceed 500 characters")
        @Schema(description = "Change reason.", example = "Employment assignment update")
        String changeReason

) {
}