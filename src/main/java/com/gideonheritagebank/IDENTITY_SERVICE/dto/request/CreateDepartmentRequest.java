package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(

        @NotBlank(message = "Department code is required")
        @Size(max = 30, message = "Department code cannot exceed 30 characters")
        String departmentCode,

        @NotBlank(message = "Department name is required")
        @Size(max = 120, message = "Department name cannot exceed 120 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description

) {
}