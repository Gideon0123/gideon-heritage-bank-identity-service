package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateStaffAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateStaffAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffAddressResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffAddressService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/staff/{staffUserId}/addresses")
@PreAuthorize("hasRole('ADMIN')")
public class StaffAddressController {

    private final StaffAddressService staffAddressService;

    /*
     * ============================================================
     * ADD STAFF ADDRESS
     * ============================================================
     */

    @PostMapping
    public ResponseEntity<ApiResponse<StaffAddressResponse>> addAddress(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody CreateStaffAddressRequest requestBody,
            HttpServletRequest request
    ) {

        StaffAddressResponse response =
                staffAddressService.addAddress(
                        staffUserId,
                        requestBody
                );

        return success(
                "Staff address added successfully.",
                response,
                HttpStatus.CREATED,
                request
        );
    }

    /*
     * ============================================================
     * RETRIEVE STAFF ADDRESSES
     * ============================================================
     */

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StaffAddressResponse>>> getAddresses(
            @PathVariable UUID staffUserId,
            @PageableDefault(
                    page = 1,
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            HttpServletRequest request
    ) {
        PagedResponse<StaffAddressResponse> addresses = staffAddressService.getStaffAddresses(
                staffUserId,
                pageable
        );

        return success(
                "Staff addresses retrieved successfully.",
                addresses,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * UPDATE STAFF ADDRESS
     * ============================================================
     */

    @PatchMapping("/{addressId}")
    public ResponseEntity<ApiResponse<StaffAddressResponse>> updateAddress(
            @PathVariable UUID staffUserId,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateStaffAddressRequest requestBody,
            HttpServletRequest request
    ) {

        StaffAddressResponse response =
                staffAddressService.updateAddress(
                        staffUserId,
                        addressId,
                        requestBody
                );

        return success(
                "Staff address updated successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * DELETE STAFF ADDRESS
     * ============================================================
     */

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID staffUserId,
            @PathVariable UUID addressId,
            HttpServletRequest request
    ) {

        staffAddressService.deleteAddress(staffUserId, addressId);

        return success(
                "Staff address deleted successfully.",
                null,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * MAKE ADDRESS PRIMARY
     * ============================================================
     */

    @PatchMapping("/{addressId}/primary")
    public ResponseEntity<ApiResponse<StaffAddressResponse>> makePrimary(
            @PathVariable UUID staffUserId,
            @PathVariable UUID addressId,
            HttpServletRequest request
    ) {

        StaffAddressResponse response =
                staffAddressService.makePrimary(
                        staffUserId,
                        addressId
                );

        return success(
                "Staff address marked as primary successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * COMMON HELPER
     * ============================================================
     */

    private <T> ResponseEntity<ApiResponse<T>> success(
            String message,
            T data,
            HttpStatus status,
            HttpServletRequest request
    ) {

        String requestId = request.getHeader("X-Request-Id");

        if (requestId == null || requestId.isBlank()) {
            requestId = TraceIdUtil.generate();
        }

        ApiResponse<T> response = ApiResponse.success(
                message,
                status.value(),
                data,
                request.getRequestURI(),
                requestId
        );

        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}