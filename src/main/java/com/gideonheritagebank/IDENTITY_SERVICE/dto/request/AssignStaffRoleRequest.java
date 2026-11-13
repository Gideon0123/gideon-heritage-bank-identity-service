package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignStaffRoleRequest(

        @NotNull(message = "Staff role is required")
        StaffRole staffRole,

        @NotBlank(message = "A reason for assigning the role is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason

) {
}
