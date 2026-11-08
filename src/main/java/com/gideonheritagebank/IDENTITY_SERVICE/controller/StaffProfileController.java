package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateStaffProfileRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffProfileService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/staff-profiles")
@PreAuthorize("hasRole('ADMIN')")
public class StaffProfileController {

    private final StaffProfileService staffProfileService;

    /*
     * ============================================================
     * RETRIEVE STAFF PROFILE BY PROFILE ID
     * ============================================================
     */

    @GetMapping("/{staffProfileId}")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getStaffProfileById(
            @PathVariable UUID staffProfileId,
            HttpServletRequest request
    ) {

        StaffProfileResponse response =
                staffProfileService.getStaffProfileById(staffProfileId);

        return success(
                "Staff profile retrieved successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * RETRIEVE STAFF PROFILE BY USER ID
     * ============================================================
     */

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getStaffProfileByUserId(
            @PathVariable UUID userId,
            HttpServletRequest request
    ) {

        StaffProfileResponse response =
                staffProfileService.getStaffProfileByUserId(userId);

        return success(
                "Staff profile retrieved successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * UPDATE STAFF PROFILE
     * ============================================================
     */

    @PatchMapping("/{staffProfileId}")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateStaffProfile(
            @PathVariable UUID staffProfileId,
            @Valid @RequestBody UpdateStaffProfileRequest requestBody,
            HttpServletRequest request
    ) {

        StaffProfileResponse response =
                staffProfileService.updateStaffProfile(
                        staffProfileId,
                        requestBody
                );

        return success(
                "Staff profile updated successfully.",
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