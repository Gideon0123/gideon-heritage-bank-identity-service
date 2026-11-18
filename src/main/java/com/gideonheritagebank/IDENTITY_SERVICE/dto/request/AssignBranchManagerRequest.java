package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignBranchManagerRequest(

        @NotNull(message = "Branch manager ID is required")
        @Schema(description = "Branch manager id.")
        UUID branchManagerId

) {
}