package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerProfileRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerAddressResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerAddressService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerProfileService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerProfileResponse profile = customerProfileService.getMyProfile(customerId);
        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer profile retrieved successfully",
                HttpStatus.OK.value(),
                profile,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerProfileResponse profile =
                customerProfileService.updateMyProfile(customerId, request);

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer profile Updated successfully",
                HttpStatus.OK.value(),
                profile,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    // =========================
    // CUSTOMER ADDRESSES
    // =========================

    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        List<CustomerAddressResponse> addresses =
                customerAddressService.getMyAddresses(customerId);

        ApiResponse<List<CustomerAddressResponse>> response = ApiResponse.success(
                "Customer addresses retrieved successfully",
                HttpStatus.OK.value(),
                addresses,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCustomerAddressRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerAddressResponse address =
                customerAddressService.addAddress(customerId, request);

        ApiResponse<CustomerAddressResponse> response = ApiResponse.success(
                "Customer address added successfully",
                HttpStatus.CREATED.value(),
                address,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> updateAddress(
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

        ApiResponse<CustomerAddressResponse> response = ApiResponse.success(
                "Customer address updated successfully",
                HttpStatus.CREATED.value(),
                address,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        customerAddressService.deleteAddress(customerId, addressId);

        ApiResponse<Void> response = ApiResponse.success(
                "Customer address deleted successfully",
                HttpStatus.NO_CONTENT.value(),
                null,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/addresses/{addressId}/primary")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> makePrimary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerAddressResponse address =
                customerAddressService.makePrimary(customerId, addressId);

        ApiResponse<CustomerAddressResponse> response = ApiResponse.success(
                "Primary Address Updated successfully",
                HttpStatus.CREATED.value(),
                address,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
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

}