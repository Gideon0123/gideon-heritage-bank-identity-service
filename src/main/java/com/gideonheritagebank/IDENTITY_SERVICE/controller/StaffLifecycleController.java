package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CloseStaffRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.DeactivateStaffRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.LockStaffRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.SuspendStaffRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffLifecycleResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffLifecycleService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/staff/{staffUserId}/lifecycle")
@PreAuthorize("hasRole('ADMIN')")
public class StaffLifecycleController {

    private final StaffLifecycleService staffLifecycleService;

    /* GET STAFF LIFECYCLE STATUS */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> getStatus(
            @PathVariable UUID staffUserId,
            HttpServletRequest request
    ) {
        StaffLifecycleResponse response = staffLifecycleService.getStaffLifecycleStatus(staffUserId);

        return success(
                "Staff lifecycle status retrieved successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* ENABLE STAFF IDENTITY */
    @PatchMapping("/enable")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> enableStaff(
            @PathVariable UUID staffUserId,
            HttpServletRequest request
    ) {
        StaffLifecycleResponse response = staffLifecycleService.enableStaff(staffUserId);

        return success(
                "Staff identity enabled successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* DISABLE STAFF IDENTITY */
    @PatchMapping("/disable")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> disableStaff(
            @PathVariable UUID staffUserId,
            HttpServletRequest request
    ) {
        StaffLifecycleResponse response = staffLifecycleService.disableStaff(staffUserId);

        return success(
                "Staff identity disabled successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* SUSPEND STAFF IDENTITY */
    @PatchMapping("/suspend")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> suspendStaff(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody SuspendStaffRequest requestBody,
            HttpServletRequest request
    ) {
        StaffLifecycleResponse response = staffLifecycleService.suspendStaff(
                staffUserId,
                requestBody
        );

        return success(
                "Staff identity suspended successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* UNSUSPEND STAFF IDENTITY */
    @PatchMapping("/unsuspend")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> unsuspendStaff(
            @PathVariable UUID staffUserId,
            HttpServletRequest request
    ) {
        StaffLifecycleResponse response = staffLifecycleService.unsuspendStaff(staffUserId);

        return success(
                "Staff identity suspension removed successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* LOCK STAFF IDENTITY */
    @PatchMapping("/lock")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> lockStaff(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody LockStaffRequest requestBody,
            HttpServletRequest request
    ) {
        StaffLifecycleResponse response = staffLifecycleService.lockStaff(
                staffUserId,
                requestBody
        );

        return success(
                "Staff identity locked successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* UNLOCK STAFF IDENTITY */
    @PatchMapping("/unlock")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> unlockStaff(
            @PathVariable UUID staffUserId,
            HttpServletRequest request
    ) {
        StaffLifecycleResponse response = staffLifecycleService.unlockStaff(staffUserId);

        return success(
                "Staff identity unlocked successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* DEACTIVATE STAFF IDENTITY */
    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> deactivateStaff(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody DeactivateStaffRequest requestBody,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID changedByUserId = getAuthenticatedUserId(jwt);

        StaffLifecycleResponse response = staffLifecycleService.deactivateStaff(
                staffUserId,
                requestBody,
                changedByUserId
        );

        return success(
                "Staff employment deactivated successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* CLOSE STAFF IDENTITY */
    @PatchMapping("/close")
    public ResponseEntity<ApiResponse<StaffLifecycleResponse>> closeStaff(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody CloseStaffRequest requestBody,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID adminUserId = getAuthenticatedUserId(jwt);

        StaffLifecycleResponse response = staffLifecycleService.closeStaff(
                staffUserId,
                requestBody,
                adminUserId
        );

        return success(
                "Staff identity closed successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /* AUTHENTICATED ADMINISTRATOR */
    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new BadRequest("Authenticated administrator identity is unavailable");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new BadRequest("Authenticated administrator identity is invalid");
        }
    }

    /* COMMON RESPONSE HELPER */
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
