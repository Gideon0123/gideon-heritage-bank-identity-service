package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        @Schema(description = "Id.")
        UUID id,
        @Schema(description = "Customer number.")
        String customerNumber,
        @Schema(description = "First name.", example = "Ada")
        String firstName,
        @Schema(description = "Last name.", example = "Okafor")
        String lastName,
        @Schema(description = "Email.", example = "customer@example.com")
        String email,
        @Schema(description = "Phone number.", example = "+2348012345678")
        String phoneNumber,
        @Schema(description = "Profile photo url.", example = "https://cdn.example.com/profile.jpg")
        String profilePhotoUrl,
        @Schema(description = "User type.")
        UserType userType,
        @Schema(description = "Enabled.")
        boolean enabled,
        @Schema(description = "Identity status.")
        CustomerIdentityStatus identityStatus,
        @Schema(description = "Email verified.")
        boolean emailVerified,
        @Schema(description = "Created at.")
        Instant createdAt,
        @Schema(description = "Updated at.")
        Instant updatedAt
) {
}