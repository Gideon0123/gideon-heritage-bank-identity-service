package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.IdempotencyRecord;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.IdempotencyResult;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RequestFingerprint;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.IdempotencyState;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.IdempotencyStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IdempotencyConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IdempotencyProcessingException;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.IdempotencyRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.IdempotencyStateResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final FingerprintService fingerprintService;
    private final ObjectMapper objectMapper;
    private final IdempotencyStateResolver stateResolver;

    public IdempotencyResult begin(
            String idempotencyKey,
            RequestFingerprint fingerprint,
            Duration ttl
    ) {
        if (fingerprint == null || fingerprint.getUserId() == null) {
            throw new IllegalArgumentException("Authenticated user is required for idempotency.");
        }
        validateTtl(ttl);

        UUID userId = fingerprint.getUserId();
        String fingerprintHash = fingerprintService.generate(fingerprint);
        UUID processingToken = UUID.randomUUID();
        Instant now = Instant.now();

        IdempotencyRecord processingRecord = IdempotencyRecord.builder()
                .userId(userId)
                .key(idempotencyKey)
                .fingerprint(fingerprintHash)
                .processingToken(processingToken)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(now)
                .expiresAt(now.plus(ttl))
                .build();

        if (repository.createIfAbsent(userId, idempotencyKey, processingRecord, ttl)) {
            return IdempotencyResult.builder()
                    .proceed(true)
                    .record(processingRecord)
                    .build();
        }

        Optional<IdempotencyRecord> existing = repository.find(userId, idempotencyKey);

        // The key may have expired between SET-NX and GET. Retry creation once.
        if (existing.isEmpty()) {
            if (repository.createIfAbsent(userId, idempotencyKey, processingRecord, ttl)) {
                return IdempotencyResult.builder()
                        .proceed(true)
                        .record(processingRecord)
                        .build();
            }
            existing = repository.find(userId, idempotencyKey);
        }

        IdempotencyState state = stateResolver.resolve(existing, fingerprintHash);
        return switch (state) {
            case NOT_FOUND -> throw new IdempotencyProcessingException(
                    "The idempotency record changed while the request was being initialized. Retry with the same key."
            );
            case PROCESSING -> throw new IdempotencyProcessingException(
                    "A request using this idempotency key is already being processed."
            );
            case FINGERPRINT_MISMATCH -> throw new IdempotencyConflictException(
                    "This Idempotency-Key has already been used with a different request."
            );
            case COMPLETED -> IdempotencyResult.builder()
                    .completed(true)
                    .record(existing.orElseThrow())
                    .build();
        };
    }

    public void complete(
            UUID userId,
            String key,
            UUID processingToken,
            Object response,
            Duration ttl
    ) throws JsonProcessingException {
        IdempotencyRecord record = repository.find(userId, key)
                .orElseThrow(() -> new IdempotencyProcessingException(
                        "The idempotency reservation expired before completion."
                ));

        if (!processingToken.equals(record.getProcessingToken())) {
            throw new IdempotencyProcessingException(
                    "This request no longer owns the idempotency reservation."
            );
        }

        if (!(response instanceof ResponseEntity<?> responseEntity)) {
            throw new IllegalStateException(
                    "Methods annotated with @Idempotent must return ResponseEntity<?>."
            );
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            fail(userId, key, processingToken);
            return;
        }

        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setHttpStatus(responseEntity.getStatusCode().value());
        MediaType contentType = responseEntity.getHeaders().getContentType();
        record.setContentType(contentType == null
                ? MediaType.APPLICATION_JSON_VALUE
                : contentType.toString());
        record.setResponseBody(responseEntity.getBody() == null
                ? null
                : objectMapper.writeValueAsString(responseEntity.getBody()));
        record.setResponseHeaders(extractReplayableHeaders(responseEntity.getHeaders()));
        record.setCompletedAt(Instant.now());
        record.setExpiresAt(Instant.now().plus(ttl));

        boolean updated = repository.completeIfOwner(
                userId, key, processingToken, record, ttl
        );
        if (!updated) {
            throw new IdempotencyProcessingException(
                    "The idempotency reservation expired or was replaced before completion."
            );
        }
    }

    public void fail(UUID userId, String key, UUID processingToken) {
        repository.deleteIfOwner(userId, key, processingToken);
    }

    public ResponseEntity<String> buildCachedResponse(IdempotencyRecord record) {
        if (record == null
                || record.getStatus() != IdempotencyStatus.COMPLETED
                || record.getHttpStatus() == null) {
            throw new IdempotencyProcessingException(
                    "The completed idempotency response is unavailable."
            );
        }

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(record.getContentType());
        } catch (Exception exception) {
            contentType = MediaType.APPLICATION_JSON;
        }

        HttpHeaders headers = new HttpHeaders();
        if (record.getResponseHeaders() != null) {
            record.getResponseHeaders().forEach(headers::put);
        }
        headers.setContentType(contentType);

        return new ResponseEntity<>(
                record.getResponseBody(),
                headers,
                HttpStatusCode.valueOf(record.getHttpStatus())
        );
    }

    private Map<String, List<String>> extractReplayableHeaders(HttpHeaders source) {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.forEach((name, values) -> {
            if (!name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
                    && !name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)
                    && !name.equalsIgnoreCase(HttpHeaders.CONNECTION)
                    && !name.equalsIgnoreCase(HttpHeaders.DATE)
                    && !name.equalsIgnoreCase(HttpHeaders.SET_COOKIE)) {
                headers.put(name, List.copyOf(values));
            }
        });
        return headers;
    }

    private void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Idempotency TTL must be greater than zero.");
        }
    }
}
