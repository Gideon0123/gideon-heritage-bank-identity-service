package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateCustomerRequest(

        @NotBlank(message = "First name is required.")
        @Size(max = 100, message = "First name cannot exceed 100 characters.")
        @Schema(description = "First name.", example = "Ada")
        String firstName,

        @NotBlank(message = "Last name is required.")
        @Size(max = 100, message = "Last name cannot exceed 100 characters.")
        @Schema(description = "Last name.", example = "Okafor")
        String lastName,

        @NotBlank(message = "Phone number is required.")
        @Size(max = 30, message = "Phone number cannot exceed 30 characters.")
        @Pattern(
                regexp = "^\\+?[0-9\\s()-]{7,30}$",
                message = "Phone number format is invalid."
        )
        @Schema(description = "Phone number.", example = "+2348012345678")
        String phoneNumber,

        @Size(
                max = 1000,
                message = "Profile photo URL cannot exceed 1000 characters."
        )
        @Schema(description = "Profile photo url.", example = "https://cdn.example.com/profile.jpg")
        String profilePhotoUrl,

        @NotBlank(message = "An update reason is required.")
        @Size(
                min = 5,
                max = 1000,
                message = "Reason must contain between 5 and 1000 characters."
        )
        @Schema(description = "Reason.", example = "Administrative review")
        String reason

) {
}
