package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.*;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.AdminCustomerService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.IdentityService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.Idempotent;
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

@Tag(name = "Customer Administration", description = "Administrative customer lookup, search, profile updates and identity controls.")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Enable customer",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
    @PostMapping("/{customerId}/enable")
    @Idempotent(ttlMinutes = 3)
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

    @Operation(
            summary = "Disable customer",
            description = "Administrative customer lookup, search, profile updates and identity controls."

    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
    @PostMapping("/{customerId}/disable")
    @Idempotent(ttlMinutes = 3)
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

    @Operation(
            summary = "Lock customer",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
    @PostMapping("/{customerId}/lock")
    @Idempotent(ttlMinutes = 3)
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

    @Operation(
            summary = "Unlock customer",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
    @PostMapping("/{customerId}/unlock")
    @Idempotent(ttlMinutes = 3)
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

    @Operation(
            summary = "Suspend customer",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
    @PostMapping("/{customerId}/suspend")
    @Idempotent(ttlMinutes = 3)
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

    @Operation(
            summary = "Unsuspend customer",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "500",

                    description = "An unexpected server error occurred.",

                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
    @PostMapping("/{customerId}/unsuspend")
    @Idempotent(ttlMinutes = 3)
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

    @Operation(
            summary = "Update customer",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
    @PutMapping("/{customerId}")
    @Idempotent(ttlMinutes = 3)
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
    @Operation(
            summary = "Get customers",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410",
                    description = "The requested verification or workflow token has expired.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred."
            )
    })
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
    @Operation(
            summary = "Get customer by id",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410",
                    description = "The requested verification or workflow token has expired.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred."
            )
    })
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

    @Operation(
            summary = "Search customers",
            description = "Administrative customer lookup, search, profile updates and identity controls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Operation completed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))
            )

    })
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