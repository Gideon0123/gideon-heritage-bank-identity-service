package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RegisterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ResendVerificationRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.VerifyEmailRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.EmailVerificationService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.IdentityService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.Idempotent;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Identity Registration and Verification", description = "Customer identity registration, email verification and verification-email resending.")
@RestController
@RequestMapping({"/auth", "/api/v1/identity/auth", "/api/v1/identity"})
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;
    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "Register customer",
            description = "Customer identity registration, email verification and verification-email resending."
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
    @PostMapping("/register")
//    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<IdentityResponse>> registerCustomer(
            @Valid @RequestBody RegisterRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        IdentityResponse identity = identityService.registerCustomer(requestBody);

        ApiResponse<IdentityResponse> response = ApiResponse.success(
                "Account created. Verify your email before logging in. " +
                        "Email delivery may be delayed; request another " +
                        "verification email if necessary.",
                HttpStatus.CREATED.value(),
                identity,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore()).body(response);
    }

    @Operation(
            summary = "Verify email",
            description = "Customer identity registration, email verification and verification-email resending."
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
    @PostMapping("/verify-email")
//    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        emailVerificationService.verifyEmail(requestBody.token());

        ApiResponse<Void> response = ApiResponse.success(
                "Email verified successfully. You can now log in if your account is enabled.",
                HttpStatus.OK.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @Operation(
            summary = "Resend verification",
            description = "Customer identity registration, email verification and verification-email resending."
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
    @PostMapping("/resend-verification")
//    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        emailVerificationService.resendVerification(
                requestBody.email()
        );

        /*
         * Deliberately generic: don't reveal whether an email is registered,
         * verified, or eligible for a verification email.
         */
        ApiResponse<Void> response = ApiResponse.success(
                "If an eligible unverified customer account exists for that email, " +
                        "a new verification email will be requested.",
                HttpStatus.ACCEPTED.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(response);
    }

}