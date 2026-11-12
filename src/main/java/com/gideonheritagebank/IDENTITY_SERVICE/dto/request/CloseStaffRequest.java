package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CloseStaffRequest(

        @NotBlank(message = "Closure reason is required")
        @Size(max = 1000, message = "Closure reason cannot exceed 1000 characters")
        String reason,

        @NotNull(message = "Retention review date is required")
        @Future(message = "Retention review date must be in the future")
        Instant retentionReviewAt

) {
}
