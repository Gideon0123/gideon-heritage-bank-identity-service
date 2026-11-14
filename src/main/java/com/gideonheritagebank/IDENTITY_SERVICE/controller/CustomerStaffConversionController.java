package com.gideonheritagebank.IDENTITY_SERVICE.controller;


import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ConvertCustomerToStaffRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerStaffConversionResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerStaffConversionService;
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
@RequestMapping("/api/v1/identity/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CustomerStaffConversionController {

    private final CustomerStaffConversionService conversionService;

    @PostMapping("/{customerUserId}/convert-to-staff")
    public ResponseEntity<ApiResponse<CustomerStaffConversionResponse>> convertCustomerToStaff(
            @PathVariable UUID customerUserId,
            @Valid @RequestBody ConvertCustomerToStaffRequest staffRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID adminUserId = getAuthenticatedUserId(jwt);

        CustomerStaffConversionResponse response = conversionService.convertCustomerToStaff(
                customerUserId,
                staffRequest,
                adminUserId
        );

        return success(
                "Customer successfully converted to staff.",
                response,
                HttpStatus.OK,
                request
        );
    }

    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new BusinessException(
                    "Authenticated administrator identity is unavailable."
            );
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Authenticated administrator identity is invalid."
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
