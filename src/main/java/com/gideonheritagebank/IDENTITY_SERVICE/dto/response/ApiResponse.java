package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiResponse", description = "Standard API response envelope used for successful and failed requests.")
public class ApiResponse<T> {

    @Builder.Default

    @Schema(description = "UTC timestamp when the response was generated.", example = "2026-11-18T12:00:00Z", name = "timestamp")
    private Instant timestamp = Instant.now();
    @Schema(description = "Indicates whether the request completed successfully.", example = "true", name = "success")
    private boolean success;
    @Schema(description = "HTTP status code returned by the API.", example = "200", name = "status")
    private int status;
    @Schema(description = "Human-readable response message.", example = "Request completed successfully.", name = "message")
    private String message;
    @Schema(description = "Response payload. Its structure depends on the endpoint.", example = "Endpoint-specific response payload", name = "data")
    private T data;
    @Schema(description = "Field-level or machine-readable errors. Empty when no field errors are present.", example = "{}", name = "errors")
    private Map<String, String> errors;
    @Schema(description = "Request path that produced this response.", example = "/api/v1/identity/customers/me", name = "path")
    private String path;
    @Schema(description = "Request trace identifier. Use it when reporting an issue.", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", name = "traceId")
    private String traceId;

    public static <T> ApiResponse<T> success(
            String message,
            int status,
            T data,
            String path,
            String traceId
    ) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .success(true)
                .status(status)
                .message(message)
                .data(data)
                .path(path)
                .traceId(traceId)
                .build();
    }

    public static <T> ApiResponse<T> failure(
            String message,
            int status,
            Map<String, String> errors,
            String path,
            String traceId
    ) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .success(false)
                .message(message)
                .errors(errors == null ? Map.of() : errors)
                .path(path)
                .traceId(traceId)
                .build();
    }
}
