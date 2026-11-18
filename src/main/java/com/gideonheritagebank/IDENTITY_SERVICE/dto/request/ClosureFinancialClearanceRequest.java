package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClosureFinancialClearanceRequest(

        @NotBlank(message = "A clearance reference is required.")
        @Size(max = 150)
        @Schema(description = "Reference.")
        String reference,

        @Size(max = 1000)
        @Schema(description = "Notes.")
        String notes

) {

}