package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;

import java.time.Instant;
import java.util.UUID;

public record StaffAddressResponse(

        @Schema(description = "Id.")
        UUID id,
        @Schema(description = "Address line1.")
        String addressLine1,
        @Schema(description = "Address line2.")
        String addressLine2,
        @Schema(description = "City.", example = "Lagos")
        String city,
        @Schema(description = "State or region.", example = "Lagos State")
        String stateOrRegion,
        @Schema(description = "Postal code.", example = "100001")
        String postalCode,
        @Schema(description = "Country code.", example = "NG")
        String countryCode,
        @Schema(description = "Address type.")
        AddressType addressType,
        @Schema(description = "Primary.")
        boolean primary,
        @Schema(description = "Created at.")
        Instant createdAt,
        @Schema(description = "Updated at.")
        Instant updatedAt

) {
}