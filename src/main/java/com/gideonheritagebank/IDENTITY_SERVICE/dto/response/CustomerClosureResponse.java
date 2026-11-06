package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerClosureStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerClosureResponse(

        UUID id,
        UUID customerId,
        String customerNumber,
        String customerName,
        CustomerClosureStatus status,
        String customerReason,
        Instant requestedAt,
        UUID reviewedBy,
        Instant reviewedAt,
        String reviewNotes,
        String financialClearanceReference,
        Instant financialClearanceConfirmedAt,
        String rejectionReason,
        Instant rejectedAt,
        Instant cancelledAt,
        UUID completedBy,
        Instant completedAt

) {
}