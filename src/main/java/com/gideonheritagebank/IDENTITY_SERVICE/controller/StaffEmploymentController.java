package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateStaffEmploymentRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffEmploymentHistoryResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffEmploymentService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/staff/{staffProfileId}/employment")
@PreAuthorize("hasRole('ADMIN')")
public class StaffEmploymentController {

    private final StaffEmploymentService staffEmploymentService;

    /*
     * ============================================================
     * UPDATE STAFF EMPLOYMENT
     * ============================================================
     */

    @PatchMapping
    public ResponseEntity<ApiResponse<StaffEmploymentHistoryResponse>> updateEmployment(
            @PathVariable UUID staffProfileId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateStaffEmploymentRequest requestBody,
            HttpServletRequest request
    ) {
        UUID changedByUserId = getAuthenticatedUserId(jwt);

        StaffEmploymentHistoryResponse response =
                staffEmploymentService.updateEmployment(
                        staffProfileId,
                        requestBody,
                        changedByUserId
                );

        return success(
                "Staff employment information updated successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * RETRIEVE EMPLOYMENT HISTORY
     * ============================================================
     */

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StaffEmploymentHistoryResponse>>> getEmploymentHistory(
            @PathVariable UUID staffProfileId,
            @PageableDefault(
                    page = 1,
                    size = 20,
                    sort = "effectiveFrom",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            HttpServletRequest request
    ) {
        PagedResponse<StaffEmploymentHistoryResponse> history =
                staffEmploymentService.getEmploymentHistory(
                        staffProfileId,
                        pageable
                );

        return success(
                "Staff employment history retrieved successfully.",
                history,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * RETRIEVE CURRENT EMPLOYMENT
     * ============================================================
     */

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<StaffEmploymentHistoryResponse>> getCurrentEmployment(
            @PathVariable UUID staffProfileId,
            HttpServletRequest request
    ) {

        StaffEmploymentHistoryResponse employment =
                staffEmploymentService.getCurrentEmployment(staffProfileId);

        return success(
                "Current staff employment information retrieved successfully.",
                employment,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * COMMON HELPERS
     * ============================================================
     */

    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new BusinessException(
                    "Authenticated staff identity is unavailable."
            );
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Authenticated staff identity is invalid."
            );
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
