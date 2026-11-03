package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCustomerAddressRequest(

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "State or region is required")
        @Size(max = 100)
        String stateOrRegion,

        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2)
        String countryCode,

        @NotNull(message = "Address type is required")
        AddressType addressType,

        Boolean primary

) {
}
