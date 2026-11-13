package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemoveStaffRoleRequest(

        @NotBlank(message = "A reason for removing the role is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason

) {
}

