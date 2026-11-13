package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRoleChangeType;

import java.time.Instant;
import java.util.UUID;

public record StaffRoleHistoryResponse(

        UUID id,
        UUID staffUserId,
        String employeeNumber,
        StaffRole previousRole,
        StaffRole newRole,
        StaffRoleChangeType changeType,
        String changeReason,
        UUID changedByUserId,
        Instant changedAt

) {
}
