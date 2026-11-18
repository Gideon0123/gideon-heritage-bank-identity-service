package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStaffAddressRequest(

        @Size(max = 255)
        @Schema(description = "Address line1.")
        String addressLine1,

        @Size(max = 255)
        @Schema(description = "Address line2.")
        String addressLine2,

        @Size(max = 100)
        @Schema(description = "City.", example = "Lagos")
        String city,

        @Size(max = 100)
        @Schema(description = "State or region.", example = "Lagos State")
        String stateOrRegion,

        @Size(max = 30)
        @Schema(description = "Postal code.", example = "100001")
        String postalCode,

        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must contain exactly two letters"
        )
        @Schema(description = "Country code.", example = "NG")
        String countryCode,

        @Schema(description = "Address type.")
        AddressType addressType,

        @Schema(description = "Primary.")
        Boolean primary

) {
}