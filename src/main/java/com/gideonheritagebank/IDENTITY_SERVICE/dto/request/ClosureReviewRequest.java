package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.Size;

public record ClosureReviewRequest(

        @Size(max = 1000)
        String notes

) {
}