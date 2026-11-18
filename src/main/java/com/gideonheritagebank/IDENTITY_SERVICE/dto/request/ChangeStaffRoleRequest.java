package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStaffRoleRequest(

        @NotNull(message = "New staff role is required")
        @Schema(description = "New staff role.")
        StaffRole newStaffRole,

        @NotBlank(message = "A reason for changing the role is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        @Schema(description = "Reason.", example = "Administrative review")
        String reason

) {
}