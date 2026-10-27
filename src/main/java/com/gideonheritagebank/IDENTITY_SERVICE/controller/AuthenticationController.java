package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.internal.AuthenticationResult;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.LoginRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

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

    /*
     * ============================================================
     * CSRF TOKEN
     * ============================================================
     *
     * Call this endpoint before making cookie-authenticated POST,
     * PUT, PATCH, or DELETE requests.
     */
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> csrf(
            CsrfToken csrfToken,
            HttpServletRequest request
    ) {

        Map<String, String> data = Map.of(
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName(),
                "token", csrfToken.getToken()
        );

        ApiResponse<Map<String, String>> response =
                ApiResponse.success(
                        "CSRF token generated",
                        200,
                        data,
                        traceId(request)
                );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    /*
     * ============================================================
     * LOGIN
     * ============================================================
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<IdentityResponse>> login(
            @Valid @RequestBody LoginRequest requestBody,
            HttpServletRequest request
    ) {

        AuthenticationResult result =
                authenticationService.login(requestBody);

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

        ApiResponse<IdentityResponse> response =
                ApiResponse.success(
                        "Login successful",
                        200,
                        result.identity(),
                        traceId(request)
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /*
     * ============================================================
     * REFRESH TOKEN
     * ============================================================
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<IdentityResponse>> refresh(
            @CookieValue(
                    name = "${identity.security.cookies.refresh-name:REFRESH_TOKEN}",
                    required = false
            ) String refreshToken,
            HttpServletRequest request
    ) {

        AuthenticationResult result =
                authenticationService.refresh(refreshToken);

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

        ApiResponse<IdentityResponse> response =
                ApiResponse.success(
                        "Token refreshed successfully",
                        200,
                        result.identity(),
                        traceId(request)
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /*
     * ============================================================
     * LOGOUT
     * ============================================================
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(
                    name = "${identity.security.cookies.refresh-name:REFRESH_TOKEN}",
                    required = false
            ) String refreshToken,
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

        ApiResponse<Void> response =
                ApiResponse.success(
                        "Logged out successfully",
                        200,
                        null,
                        traceId(request)
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

    /*
     * ============================================================
     * COOKIE HELPERS
     * ============================================================
     */

    private ResponseCookie createCookie(
            String name,
            String value,
            String path,
            Instant expiresAt
    ) {

        Duration maxAge = Duration.between(
                Instant.now(),
                expiresAt
        );

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

    private String traceId(HttpServletRequest request) {

        String traceId = request.getHeader("X-Request-Id");

        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return traceId;
    }
}