package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.AssignStaffRoleRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ChangeStaffRoleRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RemoveStaffRoleRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffRoleHistoryResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffRoleResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffRoleManagementService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/identity/admin/staff/{staffUserId}/role")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StaffRoleManagementController {

    private final StaffRoleManagementService staffRoleManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<StaffRoleResponse>> getStaffRole(
            @PathVariable UUID staffUserId,
            HttpServletRequest request
    ) {
        StaffRoleResponse response = staffRoleManagementService.getStaffRole(staffUserId);

        return success(
                "Staff role retrieved successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<StaffRoleResponse>> assignRole(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody AssignStaffRoleRequest roleRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID changedByUserId = getAuthenticatedUserId(jwt);

        StaffRoleResponse response = staffRoleManagementService.assignRole(
                staffUserId,
                roleRequest,
                changedByUserId
        );

        return success(
                "Staff role assigned successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    @PatchMapping("/change")
    public ResponseEntity<ApiResponse<StaffRoleResponse>> changeRole(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody ChangeStaffRoleRequest roleRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID changedByUserId = getAuthenticatedUserId(jwt);

        StaffRoleResponse response = staffRoleManagementService.changeRole(
                staffUserId,
                roleRequest,
                changedByUserId
        );

        return success(
                "Staff role changed successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }


    @PatchMapping("/remove")
    public ResponseEntity<ApiResponse<StaffRoleResponse>> removeRole(
            @PathVariable UUID staffUserId,
            @Valid @RequestBody RemoveStaffRoleRequest roleRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID changedByUserId = getAuthenticatedUserId(jwt);

        StaffRoleResponse response = staffRoleManagementService.removeRole(
                staffUserId,
                roleRequest,
                changedByUserId
        );

        return success(
                "Staff role removed successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }


    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<StaffRoleHistoryResponse>>> getStaffRoleHistory(
            @PathVariable UUID staffUserId,
            @PageableDefault(
                    page = 1,
                    size = 20,
                    sort = "changedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            HttpServletRequest request
    ) {
        Page<StaffRoleHistoryResponse> history = staffRoleManagementService.getStaffRoleHistory(
                staffUserId,
                pageable
        );

        PagedResponse<StaffRoleHistoryResponse> response = PagedResponse.from(history);

        return success(
                "Staff role history retrieved successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new BadRequest("Authenticated administrator identity is unavailable.");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new BadRequest("Authenticated administrator identity is invalid.");
        }
    }

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
