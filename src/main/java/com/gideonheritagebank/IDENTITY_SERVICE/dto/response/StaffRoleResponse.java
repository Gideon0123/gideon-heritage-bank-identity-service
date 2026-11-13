package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;

import java.time.Instant;
import java.util.UUID;

public record StaffRoleResponse(

        UUID staffUserId,
        String employeeNumber,
        StaffRole staffRole,
        String genericRole,
        Instant updatedAt
) {
}

