package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;

import java.time.Instant;
import java.util.UUID;

public record StaffRoleResponse(

        @Schema(description = "Staff user id.")
        UUID staffUserId,
        @Schema(description = "Employee number.", example = "GH-EMP-0001")
        String employeeNumber,
        @Schema(description = "Staff role.")
        StaffRole staffRole,
        @Schema(description = "Generic role.")
        String genericRole,
        @Schema(description = "Updated at.")
        Instant updatedAt
) {
}