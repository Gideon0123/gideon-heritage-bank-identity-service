package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default

    private Instant timestamp = Instant.now();
    private boolean success;
    private String message;
    private T data;
    private Map<String, String> errors;
    private String path;
    private String traceId;

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> failure(
            String message,
            Map<String, String> errors,
            String path
    ) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .success(false)
                .message(message)
                .errors(errors == null ? Map.of() : errors)
                .path(path)
                .build();
    }
}
