package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStaffProfileRequest(

        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @Pattern(
                regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Phone number must be in valid international E.164 format"
        )
        String phoneNumber,

        @Size(max = 1000, message = "Profile photo URL cannot exceed 1000 characters")
        String profilePhotoUrl

) {
}