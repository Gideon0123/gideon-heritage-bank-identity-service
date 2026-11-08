package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffProfileResponse(

        UUID id,
        UUID userId,

        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String profilePhotoUrl,

        String employeeNumber,
        StaffRole staffRole,
        LocalDate hireDate,
        Instant lastLoginAt,

        UUID departmentId,
        String departmentCode,
        String departmentName,

        UUID branchId,
        String jobTitle,

        EmploymentType employmentType,
        EmploymentStatus employmentStatus,

        LocalDate assignmentStartDate,
        LocalDate employmentEndDate,

        UUID reportingManagerId,

        Instant createdAt,
        Instant updatedAt

) {
}
