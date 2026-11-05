package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RegisterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ResendVerificationRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.VerifyEmailRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.EmailVerificationService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.IdentityService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/auth", "/api/v1/identity/auth", "/api/v1/identity"})
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
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

    @PostMapping("/verify-email")
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

    @PostMapping("/resend-verification")
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