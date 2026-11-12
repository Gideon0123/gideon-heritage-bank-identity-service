package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.Future;

import java.time.Instant;

public record LockStaffRequest(

        @Future(message = "Lock expiry must be a future date and time")
        Instant lockedUntil

) {
}
