package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BranchResponse(

        UUID id,

        String branchCode,
        String name,
        BranchType branchType,

        String addressLine1,
        String addressLine2,
        String city,
        String stateOrRegion,
        String postalCode,
        String countryCode,

        String phoneNumber,
        String email,

        UUID branchManagerId,
        String branchManagerName,
        String branchManagerEmail,

        BranchStatus status,

        LocalDate openedDate,
        Instant closedAt,
        String closureReason,

        Instant createdAt,
        Instant updatedAt

) {
}
