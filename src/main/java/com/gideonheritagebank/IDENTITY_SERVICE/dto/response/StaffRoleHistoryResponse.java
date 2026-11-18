package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRoleChangeType;

import java.time.Instant;
import java.util.UUID;

public record StaffRoleHistoryResponse(

        @Schema(description = "Id.")
        UUID id,
        @Schema(description = "Staff user id.")
        UUID staffUserId,
        @Schema(description = "Employee number.", example = "GH-EMP-0001")
        String employeeNumber,
        @Schema(description = "Previous role.")
        StaffRole previousRole,
        @Schema(description = "New role.")
        StaffRole newRole,
        @Schema(description = "Change type.")
        StaffRoleChangeType changeType,
        @Schema(description = "Change reason.", example = "Role or assignment change")
        String changeReason,
        @Schema(description = "Changed by user id.")
        UUID changedByUserId,
        @Schema(description = "Changed at.")
        Instant changedAt

) {
}
