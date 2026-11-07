package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CompleteStaffOnboardingRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffOnboardingService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/staff/onboarding")
@RequiredArgsConstructor
public class StaffOnboardingController {

    private final StaffOnboardingService staffOnboardingService;

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(
            @Valid @RequestBody CompleteStaffOnboardingRequest requestBody,
            @RequestHeader(value = "X-Request-Id", required = false)
            String requestId,
            HttpServletRequest request
    ) {
        staffOnboardingService.completeOnboarding(
                requestBody.getToken(),
                requestBody.getNewPassword(),
                requestBody.getConfirmPassword()
        );

        ApiResponse<Void> response = ApiResponse.success(
                "Staff onboarding completed successfully. You can now log in.",
                HttpStatus.OK.value(),
                null,
                request.getRequestURI(),
                requestId != null ? requestId : TraceIdUtil.generate()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}