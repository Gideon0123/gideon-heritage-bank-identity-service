package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;
import jakarta.validation.constraints.Size;

public record UpdateCustomerAddressRequest(

        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String stateOrRegion,

        @Size(max = 20)
        String postalCode,

        @Size(min = 2, max = 2)
        String countryCode,

        AddressType addressType,

        Boolean primary

) {
}