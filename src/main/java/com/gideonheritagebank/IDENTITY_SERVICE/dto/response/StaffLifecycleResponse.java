package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffLifecycleResponse(

        UUID userId,
        UUID staffProfileId,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        StaffIdentityStatus identityStatus,
        EmploymentStatus employmentStatus,
        boolean enabled,
        boolean emailVerified,
        boolean accountNonLocked,
        Instant lockedUntil,
        int failedLoginAttempts,
        boolean suspended,
        String suspensionReason,
        Instant suspendedAt,
        boolean identityClosed,
        Instant closedAt,
        String closureReason,
        Instant retentionReviewAt,
        LocalDate hireDate,
        LocalDate employmentEndDate,
        Instant updatedAt

) {
}
