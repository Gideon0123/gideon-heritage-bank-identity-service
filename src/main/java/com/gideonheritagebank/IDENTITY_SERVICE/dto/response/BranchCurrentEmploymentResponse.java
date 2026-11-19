package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import java.util.UUID;

public record BranchCurrentEmploymentResponse(

        UUID branchId,
        String branchCode,
        String branchName,

        long totalEmployees,

        long activeEmployees,
        long pendingEmployees,
        long employeesOnLeave,
        long suspendedEmployees,

        PagedResponse<StaffProfileResponse> employees

) {
}