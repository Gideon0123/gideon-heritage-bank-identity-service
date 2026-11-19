package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.BranchCurrentEmploymentResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffEmploymentService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/branches")
@PreAuthorize("hasRole('ADMIN')")
public class BranchEmploymentController {

    private final StaffEmploymentService staffEmploymentService;

    /* GET CURRENT EMPLOYMENT IN A BRANCH */
    @GetMapping("/{branchId}/employment")
    public ResponseEntity<ApiResponse<BranchCurrentEmploymentResponse>> getCurrentEmploymentByBranch(
            @PathVariable UUID branchId,
            @PageableDefault(
                    page = 1,
                    size = 20,
                    sort = "employeeNumber",
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            HttpServletRequest request
    ) {
        BranchCurrentEmploymentResponse response = staffEmploymentService.getCurrentEmploymentByBranch(
                branchId,
                pageable
        );

        return success(
                "Current branch employment retrieved successfully.",
                response,
                HttpStatus.OK,
                request
        );
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
