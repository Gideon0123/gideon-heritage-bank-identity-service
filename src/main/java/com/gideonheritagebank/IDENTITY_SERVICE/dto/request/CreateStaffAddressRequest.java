package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffAddressRequest(

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String stateOrRegion,

        @Size(max = 30)
        String postalCode,

        @NotBlank(message = "Country code is required")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must contain exactly two letters"
        )
        String countryCode,

        @NotNull(message = "Address type is required")
        AddressType addressType,

        Boolean primary

) {
}