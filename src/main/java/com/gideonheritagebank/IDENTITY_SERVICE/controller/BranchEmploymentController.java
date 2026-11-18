package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.BranchCurrentEmploymentResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffEmploymentService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "Branch Employment Endpoints",
        description = "Manage branch employment in the system."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/branches")
@PreAuthorize("hasRole('ADMIN')")
public class BranchEmploymentController {

    private final StaffEmploymentService staffEmploymentService;

    @Operation(
            summary = "Get current employment in a branch",
            description = """
                Retrieves the current employment information for a specific bank branch.

                Only administrators can perform this operation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Current branch employment retrieved successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data."
            )
    })
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
