package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClosureFinancialClearanceRequest(

        @NotBlank(message = "A clearance reference is required.")
        @Size(max = 150)
        String reference,

        @Size(max = 1000)
        String notes

) {
}