package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BranchResponse(

        @Schema(description = "Id.")
        UUID id,

        @Schema(description = "Branch code.", example = "GH-LAG-001")
        String branchCode,
        @Schema(description = "Name.", example = "Lagos Main Branch")
        String name,
        @Schema(description = "Branch type.")
        BranchType branchType,

        @Schema(description = "Address line1.")
        String addressLine1,
        @Schema(description = "Address line2.")
        String addressLine2,
        @Schema(description = "City.", example = "Lagos")
        String city,
        @Schema(description = "State or region.", example = "Lagos State")
        String stateOrRegion,
        @Schema(description = "Postal code.", example = "100001")
        String postalCode,
        @Schema(description = "Country code.", example = "NG")
        String countryCode,

        @Schema(description = "Phone number.", example = "+2348012345678")
        String phoneNumber,
        @Schema(description = "Email.", example = "customer@example.com")
        String email,

        @Schema(description = "Branch manager id.")
        UUID branchManagerId,
        @Schema(description = "Branch manager name.")
        String branchManagerName,
        @Schema(description = "Branch manager email.")
        String branchManagerEmail,

        @Schema(description = "Status.")
        BranchStatus status,

        @Schema(description = "Opened date.", example = "2024-01-15")
        LocalDate openedDate,
        @Schema(description = "Closed at.")
        Instant closedAt,
        @Schema(description = "Closure reason.")
        String closureReason,

        @Schema(description = "Created at.")
        Instant createdAt,
        @Schema(description = "Updated at.")
        Instant updatedAt

) {
}
