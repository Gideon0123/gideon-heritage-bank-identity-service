package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(

        @NotBlank(message = "Department code is required")
        @Size(max = 30, message = "Department code cannot exceed 30 characters")
        @Schema(description = "Department code.")
        String departmentCode,

        @NotBlank(message = "Department name is required")
        @Size(max = 120, message = "Department name cannot exceed 120 characters")
        @Schema(description = "Name.", example = "Lagos Main Branch")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        @Schema(description = "Description.")
        String description

) {
}