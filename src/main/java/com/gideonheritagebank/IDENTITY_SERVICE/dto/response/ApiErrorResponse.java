package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApiErrorResponse {

    @Schema(description = "Timestamp.")
    private Instant timestamp;

    @Schema(description = "Status.")
    private int status;

    @Schema(description = "Error.")
    private String error;

    @Schema(description = "Message.")
    private String message;

    @Schema(description = "Path.")
    private String path;

    @Builder.Default
    @Schema(description = "Field errors.")
    private Map<String, String> fieldErrors = Map.of();
}