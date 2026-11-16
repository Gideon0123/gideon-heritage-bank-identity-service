package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignBranchManagerRequest(

        @NotNull(message = "Branch manager ID is required")
        UUID branchManagerId

) {
}

