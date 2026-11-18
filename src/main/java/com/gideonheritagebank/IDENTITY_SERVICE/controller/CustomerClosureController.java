package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateCustomerClosureRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerClosureResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CustomerClosureService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.Idempotent;
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

@Tag(name = "Customer Closure", description = "Customer-initiated account closure requests and closure-request status.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/customers/me/closure-requests")
public class CustomerClosureController {

    private final CustomerClosureService customerClosureService;

    @Operation(
            summary = "Request closure",
            description = "Customer-initiated account closure requests and closure-request status."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
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
    @PostMapping
    @Idempotent(ttlMinutes = 3)
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

    @Operation(
            summary = "Get my closure requests",
            description = "Customer-initiated account closure requests and closure-request status."
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

    @Operation(
            summary = "Cancel closure request",
            description = "Customer-initiated account closure requests and closure-request status."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
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
    @PostMapping("/{requestId}/cancel")
    @Idempotent(ttlMinutes = 3)
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
