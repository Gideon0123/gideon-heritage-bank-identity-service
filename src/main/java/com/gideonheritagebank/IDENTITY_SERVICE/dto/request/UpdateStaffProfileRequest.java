package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStaffProfileRequest(

        @Size(max = 100, message = "First name cannot exceed 100 characters")
        @Schema(description = "First name.", example = "Ada")
        String firstName,

        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        @Schema(description = "Last name.", example = "Okafor")
        String lastName,

        @Pattern(
                regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Phone number must be in valid international E.164 format"
        )
        @Schema(description = "Phone number.", example = "+2348012345678")
        String phoneNumber,

        @Size(max = 1000, message = "Profile photo URL cannot exceed 1000 characters")
        @Schema(description = "Profile photo url.", example = "https://cdn.example.com/profile.jpg")
        String profilePhotoUrl

) {
}