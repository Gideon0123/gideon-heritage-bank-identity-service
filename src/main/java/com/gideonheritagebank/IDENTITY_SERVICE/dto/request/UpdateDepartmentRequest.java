package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(

        @Size(max = 120, message = "Department name cannot exceed 120 characters")
        @Schema(description = "Name.", example = "Lagos Main Branch")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        @Schema(description = "Description.")
        String description,

        @Schema(description = "Active.")
        Boolean active

) {
}