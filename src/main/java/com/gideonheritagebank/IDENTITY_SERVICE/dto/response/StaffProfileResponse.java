package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchType;
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

        // DEPARTMENT
        UUID departmentId,
        String departmentCode,
        String departmentName,

        // BRANCH
        UUID branchId,
        String branchCode,
        String branchName,
        BranchType branchType,

        String branchAddressLine1,
        String branchAddressLine2,
        String branchCity,
        String branchStateOrRegion,
        String branchPostalCode,
        String branchCountryCode,

        String branchPhoneNumber,
        String branchEmail,

        UUID branchManagerId,
        String branchManagerName,

        BranchStatus branchStatus,

        // EMPLOYMENT
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
