package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerProfileRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerAddressResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerAddressService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerProfileService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ApiResponse<CustomerProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerProfileResponse profile = customerProfileService.getMyProfile(customerId);

        return success(
                "Customer profile retrieved successfully",
                HttpStatus.OK,
                profile,
                httpRequest,
                requestId
        );
    }

    @PatchMapping("/me")
    public ApiResponse<CustomerProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerProfileResponse profile =
                customerProfileService.updateMyProfile(customerId, request);

        return success(
                "Customer profile updated successfully",
                HttpStatus.OK,
                profile,
                httpRequest,
                requestId
        );
    }

    // =========================
    // CUSTOMER ADDRESSES
    // =========================

    @GetMapping("/me/addresses")
    public ApiResponse<List<CustomerAddressResponse>> getMyAddresses(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        List<CustomerAddressResponse> addresses =
                customerAddressService.getMyAddresses(customerId);

        return success(
                "Customer addresses retrieved successfully",
                HttpStatus.OK,
                addresses,
                httpRequest,
                requestId
        );
    }

    @PostMapping("/me/addresses")
    public ApiResponse<CustomerAddressResponse> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCustomerAddressRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {

        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerAddressResponse address =
                customerAddressService.addAddress(customerId, request);

        return success(
                "Customer address added successfully",
                HttpStatus.CREATED,
                address,
                httpRequest,
                requestId
        );
    }

    @PatchMapping("/me/addresses/{addressId}")
    public ApiResponse<CustomerAddressResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateCustomerAddressRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerAddressResponse address = customerAddressService.updateAddress(
                customerId,
                addressId,
                request
        );

        return success(
                "Customer address updated successfully",
                HttpStatus.OK,
                address,
                httpRequest,
                requestId
        );
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        customerAddressService.deleteAddress(customerId, addressId);

        return success(
                "Customer address deleted successfully",
                HttpStatus.OK,
                null,
                httpRequest,
                requestId
        );
    }

    @PatchMapping("/me/addresses/{addressId}/primary")
    public ApiResponse<CustomerAddressResponse> makePrimary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerAddressResponse address =
                customerAddressService.makePrimary(customerId, addressId);

        return success(
                "Primary address updated successfully",
                HttpStatus.OK,
                address,
                httpRequest,
                requestId
        );
    }

    // =========================
    // COMMON HELPERS
    // =========================

    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalStateException(
                    "Authenticated user identity is unavailable"
            );
        }

        return UUID.fromString(jwt.getSubject());
    }

    private <T> ApiResponse<T> success(
            String message,
            HttpStatus status,
            T data,
            HttpServletRequest httpRequest,
            String requestId
    ) {
        String traceId = (requestId == null || requestId.isBlank())
                ? UUID.randomUUID().toString() : requestId;

        ApiResponse<T> response = ApiResponse.success(
                message,
                status.value(),
                data,
                traceId
        );

        response.setPath(httpRequest.getRequestURI());

        return response;
    }
}