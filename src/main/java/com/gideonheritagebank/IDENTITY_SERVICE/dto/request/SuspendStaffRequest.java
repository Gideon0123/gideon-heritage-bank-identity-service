package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendStaffRequest(

        @NotBlank(message = "Suspension reason is required")
        @Size(max = 1000, message = "Suspension reason cannot exceed 1000 characters")
        @Schema(description = "Reason.", example = "Administrative review")
        String reason

) {
}