package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;

import java.time.Instant;

public record LockStaffRequest(

        @Future(message = "Lock expiry must be a future date and time")
        @Schema(description = "Locked until.")
        Instant lockedUntil

) {
}
