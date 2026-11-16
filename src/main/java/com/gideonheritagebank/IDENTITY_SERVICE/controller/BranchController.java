package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.AssignBranchManagerRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CloseBranchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateBranchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateBranchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.BranchResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.service.BranchService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.Idempotent;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
public class BranchController {

    private final BranchService branchService;

    // CREATE BRANCH
    @PostMapping
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @Valid @RequestBody CreateBranchRequest requestBody,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.createBranch(requestBody);

        return success(
                "Branch created successfully.",
                branch,
                HttpStatus.CREATED,
                request
        );
    }

    // GET BRANCHES
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BranchResponse>>> getBranches(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(required = false) String city,
            @PageableDefault(
                    page = 1, size = 20, sort = "branchCode", direction = Sort.Direction.ASC
            ) Pageable pageable,
            HttpServletRequest request
    ) {
        PagedResponse<BranchResponse> branches = branchService.getBranches(
                keyword,
                status,
                city,
                pageable
        );

        return success(
                "Branches retrieved successfully.",
                branches,
                HttpStatus.OK,
                request
        );
    }

    // GET BRANCH BY ID
    @GetMapping("/{branchId}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(
            @PathVariable UUID branchId,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.getBranchById(branchId);

        return success(
                "Branch retrieved successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }

    // GET BRANCH BY CODE
    @GetMapping("/code/{branchCode}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchByCode(
            @PathVariable String branchCode,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.getBranchByCode(branchCode);

        return success(
                "Branch retrieved successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }

    // UPDATE BRANCH
    @PatchMapping("/{branchId}")
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateBranchRequest requestBody,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.updateBranch(
                branchId,
                requestBody
        );

        return success(
                "Branch information updated successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }

    // ASSIGN BRANCH MANAGER
    @PatchMapping("/{branchId}/manager")
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<BranchResponse>> assignBranchManager(
            @PathVariable UUID branchId,
            @Valid @RequestBody AssignBranchManagerRequest requestBody,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.assignBranchManager(
                branchId,
                requestBody
        );

        return success(
                "Branch manager assigned successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }

    // REMOVE BRANCH MANAGER
    @DeleteMapping("/{branchId}/manager")
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<BranchResponse>> removeBranchManager(
            @PathVariable UUID branchId,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.removeBranchManager(branchId);

        return success(
                "Branch manager removed successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }

    // ACTIVATE BRANCH
    @PatchMapping("/{branchId}/activate")
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<BranchResponse>> activateBranch(
            @PathVariable UUID branchId,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.activateBranch(branchId);

        return success(
                "Branch activated successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }

    // DEACTIVATE BRANCH
    @PatchMapping("/{branchId}/deactivate")
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<BranchResponse>> deactivateBranch(
            @PathVariable UUID branchId,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.deactivateBranch(branchId);

        return success(
                "Branch deactivated successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }

    // PERMANENTLY CLOSE BRANCH
    @PatchMapping("/{branchId}/close")
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<BranchResponse>> closeBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody CloseBranchRequest requestBody,
            HttpServletRequest request
    ) {
        BranchResponse branch = branchService.closeBranch(
                branchId,
                requestBody
        );

        return success(
                "Branch permanently closed successfully.",
                branch,
                HttpStatus.OK,
                request
        );
    }


    // COMMON RESPONSE HELPER
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

