package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RegisterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({
        "/auth",
        "/api/v1/identity/auth",
        "/api/v1/identity"
})
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<IdentityResponse>> registerCustomer(
            @Valid @RequestBody RegisterRequest requestBody,
            HttpServletRequest request
    ) {
        IdentityResponse identity = identityService.registerCustomer(requestBody);

        ApiResponse<IdentityResponse> response = ApiResponse.success(
                "Account has been created. Email verification is required before login. Email delivery is configured, but delivery is not guaranteed.",
                HttpStatus.CREATED.value(),
                identity,
                traceId(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    private String traceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");

        return traceId == null || traceId.isBlank()
                ? UUID.randomUUID().toString()
                : traceId;
    }
}

