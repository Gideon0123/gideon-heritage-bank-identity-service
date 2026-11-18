package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record BranchCurrentEmploymentResponse(

        @Schema(description = "Branch id.")
        UUID branchId,
        @Schema(description = "Branch code.", example = "GH-LAG-001")
        String branchCode,
        @Schema(description = "Branch name.")
        String branchName,

        @Schema(description = "Total employees.")
        long totalEmployees,

        @Schema(description = "Active employees.")
        long activeEmployees,
        @Schema(description = "Pending employees.")
        long pendingEmployees,
        @Schema(description = "Employees on leave.")
        long employeesOnLeave,
        @Schema(description = "Suspended employees.")
        long suspendedEmployees,

        @Schema(description = "Employees.")
        PagedResponse<StaffProfileResponse> employees

) {
}