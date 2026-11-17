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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "Branch Endpoints",
        description = "Manage branches in the system."
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/branches")
@PreAuthorize("hasRole('ADMIN')")
public class BranchController {

    private final BranchService branchService;

    // CREATE BRANCH
    @Operation(
            summary = "Create a bank branch",
            description = """
                Creates a new bank branch.

                Only administrators can perform this operation.
                A branch manager can be assigned separately after creation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Branch created successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid branch details."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Administrator privileges are required."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Branch code already exists."
            )
    })
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
    @Operation(
            summary = "Retrieve a list of bank branches",
            description = """
                Retrieves a paginated list of bank branches.

                Optional filters can be applied for keyword, status, and city.
                Only administrators can perform this operation.
                """
    )
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
    @Operation(
            summary = "Retrieve a branch by its ID",
            description = """
                Retrieves the details of a specific bank branch using its unique ID.

                Only administrators can perform this operation.
                """
    )
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
    @Operation(
            summary = "Retrieve a branch by its code",
            description = """
                Retrieves the details of a specific bank branch using its unique code.

                Only administrators can perform this operation.
                """
    )
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
    @Operation(
            summary = "Update a branch",
            description = """
                Updates the information of a specific bank branch.

                Only administrators can perform this operation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Branch information updated successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data."
            )
    })
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
    @Operation(
            summary = "Assign a manager to a branch",
            description = """
                Assigns a manager to a specific bank branch.

                Only administrators can perform this operation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Branch manager assigned successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data."
            )
    })
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
    @Operation(
            summary = "Remove a manager from a branch",
            description = """
                Removes the manager from a specific bank branch.

                Only administrators can perform this operation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Branch manager removed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data."
            )
    })
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
    @Operation(
            summary = "Activate a branch",
            description = """
                Activates a specific bank branch.

                Only administrators can perform this operation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Branch activated successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data."
            )
    })
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
    @Operation(
            summary = "Deactivate a branch",
            description = """
                Deactivates a specific bank branch.

                Only administrators can perform this operation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Branch deactivated successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data."
            )
    })
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
    @Operation(
            summary = "Permanently close a branch",
            description = """
                Permanently closes a specific bank branch.

                Only administrators can perform this operation.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Branch permanently closed successfully."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data."
            )
    })
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

