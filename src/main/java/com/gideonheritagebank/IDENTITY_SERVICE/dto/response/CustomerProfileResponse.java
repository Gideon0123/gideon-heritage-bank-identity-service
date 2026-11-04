package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        String customerNumber,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String profilePhotoUrl,
        UserType userType,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {}