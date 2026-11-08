package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String departmentCode,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}