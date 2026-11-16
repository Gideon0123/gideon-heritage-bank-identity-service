package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateBranchRequest(

        @NotBlank(message = "Branch code is required")
        @Size(max = 50, message = "Branch code cannot exceed 50 characters")
        String branchCode,

        @NotBlank(message = "Branch name is required")
        @Size(max = 120, message = "Branch name cannot exceed 120 characters")
        String name,

        @NotNull(message = "Branch type is required")
        BranchType branchType,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 180, message = "Address line 1 cannot exceed 180 characters")
        String addressLine1,

        @Size(max = 180, message = "Address line 2 cannot exceed 180 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @NotBlank(message = "State or region is required")
        @Size(max = 100, message = "State or region cannot exceed 100 characters")
        String stateOrRegion,

        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        String postalCode,

        @NotBlank(message = "Country code is required")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must be a valid two-letter code"
        )
        String countryCode,

        @Pattern(
                regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Phone number must be in valid international E.164 format"
        )
        String phoneNumber,

        @Email(message = "A valid branch email address is required")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @NotNull(message = "Branch opening date is required")
        LocalDate openedDate

) {
}