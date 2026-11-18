package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        @Schema(description = "Id.")
        UUID id,
        @Schema(description = "Department code.")
        String departmentCode,
        @Schema(description = "Name.", example = "Lagos Main Branch")
        String name,
        @Schema(description = "Description.")
        String description,
        @Schema(description = "Active.")
        boolean active,
        @Schema(description = "Created at.")
        Instant createdAt,
        @Schema(description = "Updated at.")
        Instant updatedAt
) {
}