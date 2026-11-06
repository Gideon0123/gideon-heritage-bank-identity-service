package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ClosureFinancialClearanceRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ClosureReviewRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RejectCustomerClosureRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerClosureResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerClosureStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerClosureService;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/customer-closure-requests")
@PreAuthorize("""
        hasAnyRole(
            'ADMIN',
            'MANAGER',
            'BRANCH_MANAGER',
            'RISK_OFFICER'
        )
        """)
public class AdminCustomerClosureController {

    private final CustomerClosureService customerClosureService;

    /* RETRIEVE CLOSURE REQUESTS */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerClosureResponse>>> getClosureRequests(
            @RequestParam(required = false) CustomerClosureStatus status,
            @PageableDefault(
                    page = 1,
                    size = 20,
                    sort = "requestedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            HttpServletRequest request
    ) {
        Page<CustomerClosureResponse> result =
                customerClosureService.getClosureRequests(status, pageable);

        return success(
                "Customer closure requests retrieved successfully.",
                PagedResponse.from(result),
                HttpStatus.OK,
                request
        );
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<CustomerClosureResponse>> getClosureRequest(
            @PathVariable UUID requestId,
            HttpServletRequest request
    ) {

        CustomerClosureResponse closure =
                customerClosureService.getClosureRequest(requestId);

        return success(
                "Customer closure request retrieved successfully.",
                closure,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * BEGIN REVIEW
     * ============================================================
     */

    @PostMapping("/{requestId}/review")
    public ResponseEntity<ApiResponse<CustomerClosureResponse>> beginReview(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ClosureReviewRequest requestBody,
            HttpServletRequest request
    ) {

        UUID staffId = getAuthenticatedUserId(jwt);

        CustomerClosureResponse closure =
                customerClosureService.beginReview(
                        requestId,
                        staffId,
                        requestBody.notes()
                );

        return success(
                "Customer closure request moved to review.",
                closure,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * REQUEST FINANCIAL CLEARANCE
     * ============================================================
     */

    @PostMapping("/{requestId}/request-financial-clearance")
    public ResponseEntity<ApiResponse<CustomerClosureResponse>>
    requestFinancialClearance(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ClosureReviewRequest requestBody,
            HttpServletRequest request
    ) {

        UUID staffId = getAuthenticatedUserId(jwt);

        CustomerClosureResponse closure =
                customerClosureService.requestFinancialClearance(
                        requestId,
                        staffId,
                        requestBody.notes()
                );

        return success(
                "Financial clearance has been requested.",
                closure,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * CONFIRM FINANCIAL CLEARANCE
     * ============================================================
     */

    @PostMapping("/{requestId}/financial-clearance")
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_OFFICER')")
    public ResponseEntity<ApiResponse<CustomerClosureResponse>>
    confirmFinancialClearance(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ClosureFinancialClearanceRequest requestBody,
            HttpServletRequest request
    ) {

        UUID staffId = getAuthenticatedUserId(jwt);

        CustomerClosureResponse closure =
                customerClosureService.confirmFinancialClearance(
                        requestId,
                        staffId,
                        requestBody.reference(),
                        requestBody.notes()
                );

        return success(
                "Financial clearance recorded. Customer closure is ready for completion.",
                closure,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * REJECT CLOSURE REQUEST
     * ============================================================
     */

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<CustomerClosureResponse>> rejectClosure(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RejectCustomerClosureRequest requestBody,
            HttpServletRequest request
    ) {

        UUID staffId = getAuthenticatedUserId(jwt);

        CustomerClosureResponse closure =
                customerClosureService.rejectClosureRequest(
                        requestId,
                        staffId,
                        requestBody.reason()
                );

        return success(
                "Customer closure request rejected.",
                closure,
                HttpStatus.OK,
                request
        );
    }

    /*
     * ============================================================
     * COMPLETE CUSTOMER CLOSURE
     * ============================================================
     */

    @PostMapping("/{requestId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerClosureResponse>> completeClosure(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {

        UUID staffId = getAuthenticatedUserId(jwt);

        CustomerClosureResponse closure =
                customerClosureService.completeClosure(
                        requestId,
                        staffId
                );

        return success(
                "Customer identity closure completed successfully.",
                closure,
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
            throw new com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException(
                    "Authenticated staff identity is unavailable."
            );
        }

        return UUID.fromString(jwt.getSubject());
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
