package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCustomerAddressRequest(

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255)
        @Schema(description = "Address line1.")
        String addressLine1,

        @Size(max = 255)
        @Schema(description = "Address line2.")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        @Schema(description = "City.", example = "Lagos")
        String city,

        @NotBlank(message = "State or region is required")
        @Size(max = 100)
        @Schema(description = "State or region.", example = "Lagos State")
        String stateOrRegion,

        @Size(max = 20)
        @Schema(description = "Postal code.", example = "100001")
        String postalCode,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be a 2-letter ISO code")
        @Schema(description = "Country code.", example = "NG")
        String countryCode,

        @NotNull(message = "Address type is required")
        @Schema(description = "Address type.")
        AddressType addressType,

        @Schema(description = "Primary.")
        Boolean primary

) {
}
