package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.EmailChangeRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.VerifyEmailChangeRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.EmailChangeService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(
        name = "Email Change",
        description = "Email change requests and verification for customer and staff identities."
)
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class EmailChangeController {

    private final EmailChangeService emailChangeService;

    /* REQUEST EMAIL CHANGE */
    @Operation(
            summary = "Request email change",
            description = """
                    Allows an authenticated customer or staff member
                    to request an email address change. The new email
                    address must be verified before the identity can
                    authenticate again.
                    """
    )
    @PostMapping("/me/email-change")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(
            @Valid @RequestBody EmailChangeRequest requestBody,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId,
            HttpServletRequest request
    ) {
        UUID userId = getAuthenticatedUserId(jwt);

        emailChangeService.requestEmailChange(
                userId,
                requestBody.newEmail()
        );

        ApiResponse<Void> response = ApiResponse.success(
                "Your email change request was accepted. "
                        + "A verification link has been sent to your new email address.",
                HttpStatus.ACCEPTED.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    /* VERIFY EMAIL CHANGE */
    @Operation(
            summary = "Verify changed email",
            description = """
                    Verifies ownership of the new email address.
                    On success, the identity's email becomes verified
                    and an email-change confirmation event is published
                    to the transactional outbox.
                    """
    )
    @PostMapping("/email-change/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmailChange(
            @Valid @RequestBody VerifyEmailChangeRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId,
            HttpServletRequest request
    ) {
        emailChangeService.verifyEmailChange(
                requestBody.token()
        );

        ApiResponse<Void> response = ApiResponse.success(
                "Your new email address has been verified successfully. "
                        + "A confirmation notification has been requested.",
                HttpStatus.OK.value(),
                null,
                request.getRequestURI(),
                requestId != null
                        ? requestId
                        : TraceIdUtil.generate()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    /* JWT SUBJECT */
    private UUID getAuthenticatedUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException(
                    "Authenticated identity is unavailable."
            );
        }

        try {
            return UUID.fromString(jwt.getSubject());

        } catch (IllegalArgumentException exception) {

            throw new com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException(
                    "Authenticated identity is invalid."
            );
        }
    }
}
