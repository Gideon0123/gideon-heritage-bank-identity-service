package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateBranchRequest(

        @Schema(
                description = "Unique branch code.",
                example = "GH-LAG-001"
        )
        @NotBlank(message = "Branch code is required")
        @Size(max = 50, message = "Branch code cannot exceed 50 characters")
        String branchCode,

        @Schema(
                description = "Official branch name.",
                example = "Lagos Main Branch"
        )
        @NotBlank(message = "Branch name is required")
        @Size(max = 120, message = "Branch name cannot exceed 120 characters")
        String name,

        @Schema(
                description = "Type of banking branch.",
                example = "HEAD_OFFICE"
        )
        @NotNull(message = "Branch type is required")
        BranchType branchType,

        @Schema(
                description = "Branch address line 1.",
                example = "123 Main Street"
        )
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 180, message = "Address line 1 cannot exceed 180 characters")
        String addressLine1,

        @Schema(
                description = "Branch address line 2 (optional).",
                example = "Suite 456"
        )
        @Size(max = 180, message = "Address line 2 cannot exceed 180 characters")
        String addressLine2,

        @Schema(
                description = "City where the branch is located.",
                example = "Lagos"
        )
        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Schema(
                description = "State or region where the branch is located.",
                example = "Lagos State"
        )
        @NotBlank(message = "State or region is required")
        @Size(max = 100, message = "State or region cannot exceed 100 characters")
        String stateOrRegion,

        @Schema(
                description = "Postal code for the branch location.",
                example = "100001"
        )
        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        String postalCode,

        @Schema(
                description = "Two-letter country code (ISO 3166-1 alpha-2).",
                example = "NG"
        )
        @NotBlank(message = "Country code is required")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must be a valid two-letter code"
        )
        String countryCode,

        @Schema(
                description = "Phone number for the branch.",
                example = "+2341234567890"
        )
        @Pattern(
                regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Phone number must be in valid international E.164 format"
        )
        String phoneNumber,

        @Schema(
                description = "Email address for the branch.",
                example = "branch@company.com"
        )
        @Email(message = "A valid branch email address is required")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @Schema(
                description = "Date when the branch was opened.",
                example = "2023-01-15"
        )
        @NotNull(message = "Branch opening date is required")
        LocalDate openedDate

) {
}