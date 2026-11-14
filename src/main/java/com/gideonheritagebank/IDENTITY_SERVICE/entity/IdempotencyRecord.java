package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.IdempotencyStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    private UUID userId;
    private String key;
    private String fingerprint;
    @JsonProperty("processingToken")
    private UUID processingToken;
    private IdempotencyStatus status;
    private String responseBody;
    private Integer httpStatus;
    private Map<String, List<String>> responseHeaders;

    @Builder.Default
    private String contentType = MediaType.APPLICATION_JSON_VALUE;

    private Instant createdAt;
    private Instant completedAt;
    private Instant expiresAt;
}
