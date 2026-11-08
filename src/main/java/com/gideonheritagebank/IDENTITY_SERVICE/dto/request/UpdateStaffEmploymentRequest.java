package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateStaffEmploymentRequest(

        UUID departmentId,

        UUID branchId,

        @Size(max = 120, message = "Job title cannot exceed 120 characters")
        String jobTitle,

        EmploymentType employmentType,

        @NotNull(message = "Employment status is required")
        EmploymentStatus employmentStatus,

        @NotNull(message = "Effective date is required")
        LocalDate effectiveFrom,

        LocalDate employmentEndDate,

        UUID reportingManagerId,

        @Size(max = 500, message = "Change reason cannot exceed 500 characters")
        String changeReason

) {
}