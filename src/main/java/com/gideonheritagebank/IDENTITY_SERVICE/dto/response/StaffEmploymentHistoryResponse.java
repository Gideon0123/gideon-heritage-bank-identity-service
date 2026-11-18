package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffEmploymentHistoryResponse(

        @Schema(description = "Id.")
        UUID id,

        @Schema(description = "Department id.")
        UUID departmentId,
        @Schema(description = "Department code.")
        String departmentCode,
        @Schema(description = "Department name.")
        String departmentName,

        @Schema(description = "Branch id.")
        UUID branchId,
        @Schema(description = "Branch code.", example = "GH-LAG-001")
        String branchCode,
        @Schema(description = "Branch name.")
        String branchName,

        @Schema(description = "Job title.", example = "Branch Operations Officer")
        String jobTitle,

        @Schema(description = "Employment type.")
        EmploymentType employmentType,
        @Schema(description = "Employment status.")
        EmploymentStatus employmentStatus,

        @Schema(description = "Effective from.", example = "2026-01-01")
        LocalDate effectiveFrom,
        @Schema(description = "Effective to.")
        LocalDate effectiveTo,

        @Schema(description = "Change reason.", example = "Employment assignment update")
        String changeReason,
        @Schema(description = "Changed by user id.")
        UUID changedByUserId,

        @Schema(description = "Created at.")
        Instant createdAt

) {
}
