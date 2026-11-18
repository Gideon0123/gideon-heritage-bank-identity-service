package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffLifecycleResponse(

        @Schema(description = "User id.")
        UUID userId,
        @Schema(description = "Staff profile id.")
        UUID staffProfileId,
        @Schema(description = "Employee number.", example = "GH-EMP-0001")
        String employeeNumber,
        @Schema(description = "First name.", example = "Ada")
        String firstName,
        @Schema(description = "Last name.", example = "Okafor")
        String lastName,
        @Schema(description = "Email.", example = "customer@example.com")
        String email,
        @Schema(description = "Identity status.")
        StaffIdentityStatus identityStatus,
        @Schema(description = "Employment status.")
        EmploymentStatus employmentStatus,
        @Schema(description = "Enabled.")
        boolean enabled,
        @Schema(description = "Email verified.")
        boolean emailVerified,
        @Schema(description = "Account non locked.")
        boolean accountNonLocked,
        @Schema(description = "Locked until.")
        Instant lockedUntil,
        @Schema(description = "Failed login attempts.")
        int failedLoginAttempts,
        @Schema(description = "Suspended.")
        boolean suspended,
        @Schema(description = "Suspension reason.")
        String suspensionReason,
        @Schema(description = "Suspended at.")
        Instant suspendedAt,
        @Schema(description = "Identity closed.")
        boolean identityClosed,
        @Schema(description = "Closed at.")
        Instant closedAt,
        @Schema(description = "Closure reason.")
        String closureReason,
        @Schema(description = "Retention review at.")
        Instant retentionReviewAt,
        @Schema(description = "Hire date.")
        LocalDate hireDate,
        @Schema(description = "Employment end date.", example = "2026-11-18")
        LocalDate employmentEndDate,
        @Schema(description = "Updated at.")
        Instant updatedAt

) {
}
