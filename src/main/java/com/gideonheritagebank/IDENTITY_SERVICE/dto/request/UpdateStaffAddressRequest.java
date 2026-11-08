package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStaffAddressRequest(

        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String stateOrRegion,

        @Size(max = 30)
        String postalCode,

        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must contain exactly two letters"
        )
        String countryCode,

        AddressType addressType,

        Boolean primary

) {
}