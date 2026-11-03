package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerProfileRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerAddressResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerAddressService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/customers")
public class CustomerController {

    private final CustomerProfileService customerProfileService;
    private final CustomerAddressService customerAddressService;

    // =========================
    // CUSTOMER PROFILE
    // =========================

    @GetMapping("/me")
    public CustomerProfileResponse getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        return customerProfileService.getMyProfile(customerId);
    }

    @PatchMapping("/me")
    public CustomerProfileResponse updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        return customerProfileService.updateMyProfile(
                customerId,
                request
        );
    }

    // =========================
    // CUSTOMER ADDRESSES
    // =========================

    @GetMapping("/me/addresses")
    public List<CustomerAddressResponse> getMyAddresses(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        return customerAddressService.getMyAddresses(customerId);
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAddressResponse addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCustomerAddressRequest request
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        return customerAddressService.addAddress(
                customerId,
                request
        );
    }

    @PatchMapping("/me/addresses/{addressId}")
    public CustomerAddressResponse updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateCustomerAddressRequest request
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        return customerAddressService.updateAddress(
                customerId,
                addressId,
                request
        );
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {

        UUID customerId = getAuthenticatedUserId(jwt);

        customerAddressService.deleteAddress(
                customerId,
                addressId
        );
    }

    @PatchMapping("/me/addresses/{addressId}/primary")
    public CustomerAddressResponse makePrimary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        return customerAddressService.makePrimary(
                customerId,
                addressId
        );
    }

    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalStateException(
                    "Authenticated user identity is unavailable"
            );
        }

        return UUID.fromString(jwt.getSubject());
    }
}