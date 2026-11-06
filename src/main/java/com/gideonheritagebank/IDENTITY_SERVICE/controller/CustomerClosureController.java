package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateCustomerClosureRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerClosureResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerClosureService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/customers/me/closure-requests")
public class CustomerClosureController {

    private final CustomerClosureService customerClosureService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerClosureResponse>> requestClosure(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCustomerClosureRequest requestBody,
            HttpServletRequest request
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerClosureResponse closure = customerClosureService.requestClosure(
                customerId,
                requestBody
        );

        return success(
                "Customer closure request submitted successfully.",
                closure,
                HttpStatus.CREATED,
                request
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerClosureResponse>>> getMyClosureRequests(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        List<CustomerClosureResponse> closures =
                customerClosureService.getMyClosureRequests(customerId);

        return success(
                "Customer closure requests retrieved successfully.",
                closures,
                HttpStatus.OK,
                request
        );
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<CustomerClosureResponse>> cancelClosureRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId,
            HttpServletRequest request
    ) {
        UUID customerId = getAuthenticatedUserId(jwt);

        CustomerClosureResponse closure =
                customerClosureService.cancelMyClosureRequest(
                        customerId,
                        requestId
                );

        return success(
                "Customer closure request cancelled successfully.",
                closure,
                HttpStatus.OK,
                request
        );
    }

    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException(
                    "Authenticated customer identity is unavailable."
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