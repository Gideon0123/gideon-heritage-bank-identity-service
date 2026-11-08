package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(

        @Size(max = 120, message = "Department name cannot exceed 120 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        Boolean active

) {
}