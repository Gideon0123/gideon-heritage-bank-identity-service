package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffIdentityCreationRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({
        "/staff",
        "/api/v1/identity/staff"
})
@RequiredArgsConstructor
public class StaffIdentityController {

    private final StaffIdentityService staffIdentityService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IdentityResponse>> createStaff(
            @Valid @RequestBody StaffIdentityCreationRequest requestBody,
            HttpServletRequest request
    ) {

        IdentityResponse identity = staffIdentityService.createStaffIdentity(requestBody);

        ApiResponse<IdentityResponse> response = ApiResponse.success(
                "Staff identity created. The account remains disabled until the staff onboarding and credential-setup process is completed.",
                HttpStatus.CREATED.value(),
                identity,
                traceId(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    private String traceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");

        return traceId == null || traceId.isBlank()
                ? UUID.randomUUID().toString()
                : traceId;
    }
}

