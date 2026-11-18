package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ClosureReviewRequest(

        @Size(max = 1000)
        @Schema(description = "Notes.")
        String notes

) {

}