package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerClosureStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerClosureResponse(

        @Schema(description = "Id.")
        UUID id,
        @Schema(description = "Customer id.")
        UUID customerId,
        @Schema(description = "Customer number.")
        String customerNumber,
        @Schema(description = "Customer name.")
        String customerName,
        @Schema(description = "Status.")
        CustomerClosureStatus status,
        @Schema(description = "Customer reason.")
        String customerReason,
        @Schema(description = "Requested at.")
        Instant requestedAt,
        @Schema(description = "Reviewed by.")
        UUID reviewedBy,
        @Schema(description = "Reviewed at.")
        Instant reviewedAt,
        @Schema(description = "Review notes.")
        String reviewNotes,
        @Schema(description = "Financial clearance reference.")
        String financialClearanceReference,
        @Schema(description = "Financial clearance confirmed at.")
        Instant financialClearanceConfirmedAt,
        @Schema(description = "Rejection reason.")
        String rejectionReason,
        @Schema(description = "Rejected at.")
        Instant rejectedAt,
        @Schema(description = "Cancelled at.")
        Instant cancelledAt,
        @Schema(description = "Completed by.")
        UUID completedBy,
        @Schema(description = "Completed at.")
        Instant completedAt

) {
}