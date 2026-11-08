package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffEmploymentHistoryResponse(

        UUID id,

        UUID departmentId,
        String departmentCode,
        String departmentName,

        UUID branchId,

        String jobTitle,

        EmploymentType employmentType,

        EmploymentStatus employmentStatus,

        LocalDate effectiveFrom,
        LocalDate effectiveTo,

        String changeReason,

        UUID changedByUserId,

        Instant createdAt

) {
}