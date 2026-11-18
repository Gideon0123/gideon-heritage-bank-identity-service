package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchType;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffProfileResponse(

        @Schema(description = "Id.")
        UUID id,
        @Schema(description = "User id.")
        UUID userId,

        @Schema(description = "First name.", example = "Ada")
        String firstName,
        @Schema(description = "Last name.", example = "Okafor")
        String lastName,
        @Schema(description = "Email.", example = "customer@example.com")
        String email,
        @Schema(description = "Phone number.", example = "+2348012345678")
        String phoneNumber,
        @Schema(description = "Profile photo url.", example = "https://cdn.example.com/profile.jpg")
        String profilePhotoUrl,

        @Schema(description = "Employee number.", example = "GH-EMP-0001")
        String employeeNumber,
        @Schema(description = "Staff role.")
        StaffRole staffRole,
        @Schema(description = "Hire date.")
        LocalDate hireDate,
        @Schema(description = "Last login at.")
        Instant lastLoginAt,

        // DEPARTMENT
        @Schema(description = "Department id.")
        UUID departmentId,
        @Schema(description = "Department code.")
        String departmentCode,
        @Schema(description = "Department name.")
        String departmentName,

        // BRANCH
        @Schema(description = "Branch id.")
        UUID branchId,
        @Schema(description = "Branch code.", example = "GH-LAG-001")
        String branchCode,
        @Schema(description = "Branch name.")
        String branchName,
        @Schema(description = "Branch type.")
        BranchType branchType,

        @Schema(description = "Branch address line1.")
        String branchAddressLine1,
        @Schema(description = "Branch address line2.")
        String branchAddressLine2,
        @Schema(description = "Branch city.")
        String branchCity,
        @Schema(description = "Branch state or region.")
        String branchStateOrRegion,
        @Schema(description = "Branch postal code.")
        String branchPostalCode,
        @Schema(description = "Branch country code.")
        String branchCountryCode,

        @Schema(description = "Branch phone number.")
        String branchPhoneNumber,
        @Schema(description = "Branch email.")
        String branchEmail,

        @Schema(description = "Branch manager id.")
        UUID branchManagerId,
        @Schema(description = "Branch manager name.")
        String branchManagerName,

        @Schema(description = "Branch status.")
        BranchStatus branchStatus,

        // EMPLOYMENT
        @Schema(description = "Job title.", example = "Branch Operations Officer")
        String jobTitle,
        @Schema(description = "Employment type.")
        EmploymentType employmentType,
        @Schema(description = "Employment status.")
        EmploymentStatus employmentStatus,

        @Schema(description = "Assignment start date.")
        LocalDate assignmentStartDate,
        @Schema(description = "Employment end date.", example = "2026-11-18")
        LocalDate employmentEndDate,

        @Schema(description = "Reporting manager id.")
        UUID reportingManagerId,

        @Schema(description = "Created at.")
        Instant createdAt,
        @Schema(description = "Updated at.")
        Instant updatedAt

) {
}
