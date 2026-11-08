package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.AddressType;

import java.time.Instant;
import java.util.UUID;

public record StaffAddressResponse(

        UUID id,
        String addressLine1,
        String addressLine2,
        String city,
        String stateOrRegion,
        String postalCode,
        String countryCode,
        AddressType addressType,
        boolean primary,
        Instant createdAt,
        Instant updatedAt

) {
}