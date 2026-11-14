package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.IdempotencyStatus;
import lombok.*;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    private Long userId;
    private String key;
    private String fingerprint;
    private IdempotencyStatus status;
    private String responseBody;
    private Integer httpStatus;

    @Builder.Default
    private String contentType = MediaType.APPLICATION_JSON_VALUE;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

}