package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.internal.AuthenticationResult;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.LoginRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ForgotPasswordRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ResetPasswordRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.service.AuthenticationService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.PasswordResetService;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ChangePasswordRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.service.PasswordChangeService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "Authentication", description = "Login, token refresh, logout, password recovery and CSRF token operations.")
@RestController
@RequestMapping({"/auth", "/api/v1/identity/auth", "/api/v1/identity"})
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final PasswordChangeService passwordChangeService;

    @Value("${identity.security.cookies.access-name:ACCESS_TOKEN}")
    private String accessCookieName;

    @Value("${identity.security.cookies.refresh-name:REFRESH_TOKEN}")
    private String refreshCookieName;

    @Value("${identity.security.cookies.access-path:/}")
    private String accessCookiePath;

    @Value("${identity.security.cookies.refresh-path:/api/v1/identity/auth}")
    private String refreshCookiePath;

    @Value("${identity.security.cookies.secure:false}")
    private boolean secureCookies;

    @Value("${identity.security.cookies.same-site:Lax}")
    private String sameSite;

    @Operation(
            summary = "CSRF",
            description = "Login, token refresh, logout, password recovery and CSRF token operations."
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
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> csrf(
            CsrfToken csrfToken,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId,
            HttpServletRequest request
    ) {
        Map<String, String> data = Map.of(
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName(),
                "token", csrfToken.getToken()
        );

        ApiResponse<Map<String, String>> response = ApiResponse.success(
                "CSRF token generated",
                HttpStatus.OK.value(),
                data,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );
        response.setPath(request.getRequestURI());

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    /* LOGIN */
    @Operation(
            summary = "Login",
            description = "Login, token refresh, logout, password recovery and CSRF token operations."
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
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<IdentityResponse>> login(
            @Valid @RequestBody LoginRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId,
            HttpServletRequest request
    ) {
        AuthenticationResult result = authenticationService.login(requestBody);

        ResponseCookie accessCookie = createCookie(
                accessCookieName,
                result.accessToken(),
                accessCookiePath,
                result.accessTokenExpiresAt()
        );

        ResponseCookie refreshCookie = createCookie(
                refreshCookieName,
                result.refreshToken(),
                refreshCookiePath,
                result.refreshTokenExpiresAt()
        );

        ApiResponse<IdentityResponse> response = ApiResponse.success(
                "Login successful",
                HttpStatus.OK.value(),
                result.identity(),
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /* REFRESH TOKEN */
    @Operation(
            summary = "Refresh",
            description = "Login, token refresh, logout, password recovery and CSRF token operations."
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
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<IdentityResponse>> refresh(
            @CookieValue(
                    name = "${identity.security.cookies.refresh-name:REFRESH_TOKEN}",
                    required = false
            ) String refreshToken,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId,
            HttpServletRequest request
    ) {
        AuthenticationResult result = authenticationService.refresh(refreshToken);

        ResponseCookie accessCookie = createCookie(
                accessCookieName,
                result.accessToken(),
                accessCookiePath,
                result.accessTokenExpiresAt()
        );

        ResponseCookie refreshCookie = createCookie(
                refreshCookieName,
                result.refreshToken(),
                refreshCookiePath,
                result.refreshTokenExpiresAt()
        );

        ApiResponse<IdentityResponse> response = ApiResponse.success(
                "Token refreshed successfully",
                200,
                result.identity(),
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /* LOGOUT */
    @Operation(
            summary = "Logout",
            description = "Login, token refresh, logout, password recovery and CSRF token operations."
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
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(
                    name = "${identity.security.cookies.refresh-name:REFRESH_TOKEN}",
                    required = false
            ) String refreshToken,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId,
            HttpServletRequest request
    ) {
        authenticationService.logout(refreshToken);

        ResponseCookie expiredAccessCookie = expireCookie(
                accessCookieName,
                accessCookiePath
        );

        ResponseCookie expiredRefreshCookie = expireCookie(
                refreshCookieName,
                refreshCookiePath
        );

        ApiResponse<Void> response = ApiResponse.success(
                "Logged out successfully",
                HttpStatus.OK.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredAccessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredRefreshCookie.toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    @Operation(
            summary = "Forgot password",
            description = "Login, token refresh, logout, password recovery and CSRF token operations."
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
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody ForgotPasswordRequest requestBody,
            HttpServletRequest request
    ) {
        passwordResetService.requestPasswordReset(requestBody.email());

        /*
         * The same response is returned whether or not the email exists.
         * Do not return the token, reset URL, or account state.
         */

        ApiResponse<Void> response = ApiResponse.success(
                "If an eligible account exists for that email, "
                        + "password reset instructions will be sent.",
                HttpStatus.ACCEPTED.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.accepted()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    @Operation(
            summary = "Reset password",
            description = "Login, token refresh, logout, password recovery and CSRF token operations."
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
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        passwordResetService.resetPassword(
                requestBody.token(),
                requestBody.newPassword(),
                requestBody.confirmPassword()
        );

        ApiResponse<Void> response = ApiResponse.success(
                "Password reset successfully. You can now log in "
                        + "using your new password.",
                HttpStatus.OK.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /* CHANGE PASSWORD */
    @Operation(
            summary = "Change password",
            description = "Login, token refresh, logout, password recovery and CSRF token operations."
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
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest requestBody,
            HttpServletRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {

        /*
         * The JWT subject is the authenticated user's UUID.
         * The client cannot choose which user's password to change.
         */

        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));

        passwordChangeService.changePassword(
                userId,
                requestBody.currentPassword(),
                requestBody.newPassword(),
                requestBody.confirmPassword()
        );

        ApiResponse<Void> response = ApiResponse.success(
                "Password changed successfully. Please log in again "
                        + "with your new password.",
                HttpStatus.OK.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        ResponseCookie expiredAccessCookie = expireCookie(
                accessCookieName,
                accessCookiePath
        );

        ResponseCookie expiredRefreshCookie = expireCookie(
                refreshCookieName,
                refreshCookiePath
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredAccessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredRefreshCookie.toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /* COOKIE HELPERS */
    private ResponseCookie createCookie(
            String name,
            String value,
            String path,
            Instant expiresAt
    ) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);

        if (maxAge.isNegative() || maxAge.isZero()) {
            maxAge = Duration.ZERO;
        }

        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie expireCookie(
            String name,
            String path
    ) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }

}
