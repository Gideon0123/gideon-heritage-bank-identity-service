package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.*;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.AdminCustomerService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.IdentityService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/customers")
@PreAuthorize("""
        hasAnyRole(
            'ADMIN',
            'MANAGER',
            'BRANCH_MANAGER',
            'SUPPORT_AGENT',
            'RISK_OFFICER'
        )
        """)
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;
    private final IdentityService identityService;

    @PostMapping("/{customerId}/enable")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> enableCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerIdentityActionRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        CustomerProfileResponse customer = identityService.enableCustomer(
                customerId,
                requestBody.reason()
        );

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer Identity enabled successfully",
                HttpStatus.OK.value(),
                customer,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}/disable")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> disableCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerIdentityActionRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        CustomerProfileResponse customer = identityService.disableCustomer(
                customerId,
                requestBody.reason()
        );

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer Identity disabled successfully",
                HttpStatus.OK.value(),
                customer,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}/lock")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> lockCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody LockCustomerRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        CustomerProfileResponse customer = identityService.lockCustomer(
                customerId,
                requestBody.lockedUntil(),
                requestBody.reason()
        );

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer Identity Locked successfully",
                HttpStatus.OK.value(),
                customer,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}/unlock")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> unlockCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerIdentityActionRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        CustomerProfileResponse customer = identityService.unlockCustomer(
                customerId,
                requestBody.reason()
        );

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer Identity unlocked successfully",
                HttpStatus.OK.value(),
                customer,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}/suspend")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> suspendCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody SuspendCustomerRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        CustomerProfileResponse customer = identityService.suspendCustomer(
                customerId,
                requestBody.reason()
        );

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer Identity Suspended successfully",
                HttpStatus.OK.value(),
                customer,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}/unsuspend")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> unsuspendCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerIdentityActionRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        CustomerProfileResponse customer = identityService.unsuspendCustomer(
                customerId,
                requestBody.reason()
        );

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer Identity suspension removed successfully",
                HttpStatus.OK.value(),
                customer,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody AdminUpdateCustomerRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        CustomerProfileResponse customer = adminCustomerService.updateCustomerByStaff(
                customerId,
                requestBody
        );

        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer Profile updated successfully",
                HttpStatus.OK.value(),
                customer,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    // RETRIEVE ALL CUSTOMERS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerProfileResponse>>> getCustomers(

            @ModelAttribute CustomerFilterRequest filter,

            @PageableDefault(
                    page = 1, size = 20, sort = "createdAt", direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        Page<CustomerProfileResponse> customers =
                adminCustomerService.getCustomers(filter, pageable);

        PagedResponse<CustomerProfileResponse> pagedResponse = PagedResponse.from(customers);
        ApiResponse<PagedResponse<CustomerProfileResponse>> response = ApiResponse.success(
                "Customers retrieved successfully",
                HttpStatus.OK.value(),
                pagedResponse,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    // RETRIEVE CUSTOMER BY ID
    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCustomerById(

            @PathVariable UUID customerId,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        CustomerProfileResponse customer = adminCustomerService.getCustomerById(customerId);
        ApiResponse<CustomerProfileResponse> response = ApiResponse.success(
                "Customer retrieved successfully",
                HttpStatus.OK.value(),
                customer,
                httpRequest.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerProfileResponse>>> searchCustomers(
            @Valid @ModelAttribute CustomerSearchRequest request,
            @PageableDefault(
                    page = 1,
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest httpRequest
    ) {
        Page<CustomerProfileResponse> result =
                adminCustomerService.searchCustomers(request, pageable);

        PagedResponse<CustomerProfileResponse> pagedResponse =
                PagedResponse.from(result);

        ApiResponse<PagedResponse<CustomerProfileResponse>> response =
                ApiResponse.success(
                        "Customers retrieved successfully.",
                        HttpStatus.OK.value(),
                        pagedResponse,
                        httpRequest.getRequestURI(),
                        requestId != null ? requestId : TraceIdUtil.generate()
                );

        return ResponseEntity.ok(response);
    }

}