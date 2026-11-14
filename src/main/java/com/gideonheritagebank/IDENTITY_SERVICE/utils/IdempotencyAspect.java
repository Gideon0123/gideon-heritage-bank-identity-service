package com.gideonheritagebank.IDENTITY_SERVICE.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.CurrentUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.IdempotencyRecord;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.IdempotencyResult;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RequestFingerprint;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.MissingIdempotencyKeyException;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CurrentUserService;
import com.gideonheritagebank.IDENTITY_SERVICE.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.security.Principal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.util.HexFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final HttpServletRequest request;
    private final CurrentUserService currentUserService;
    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(
            ProceedingJoinPoint joinPoint,
            Idempotent idempotent
    ) throws Throwable {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        UUID userId = currentUser.getId();
        String key = extractKey();
        Duration ttl = resolveTtl(idempotent);
        RequestFingerprint fingerprint = buildFingerprint(joinPoint, userId);

        IdempotencyResult result = idempotencyService.begin(key, fingerprint, ttl);
        if (result.isCompleted()) {
            return idempotencyService.buildCachedResponse(result.getRecord());
        }

        IdempotencyRecord processingRecord = result.getRecord();
        UUID processingToken = processingRecord.getProcessingToken();

        try {
            Object response = joinPoint.proceed();
            idempotencyService.complete(userId, key, processingToken, response, ttl);
            return response;
        } catch (Throwable exception) {
            idempotencyService.fail(userId, key, processingToken);
            throw exception;
        }
    }

    private String extractKey() {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            throw new MissingIdempotencyKeyException(
                    "The Idempotency-Key request header is required."
            );
        }
        if (key.length() > 255 || key.chars().anyMatch(Character::isISOControl)) {
            throw new BadRequest("Idempotency-Key must be at most 255 characters and contain no control characters.");
        }
        return key.trim();
    }

    private Duration resolveTtl(Idempotent idempotent) {
        long minutes = idempotent.ttlMinutes() > 0
                ? idempotent.ttlMinutes()
                : properties.getExpirationMinutes();
        if (minutes <= 0) {
            throw new IllegalStateException("The configured idempotency TTL must be greater than zero.");
        }
        return Duration.ofMinutes(minutes);
    }

    private RequestFingerprint buildFingerprint(
            ProceedingJoinPoint joinPoint,
            UUID userId
    ) throws JsonProcessingException {
        List<Object> args = Arrays.stream(joinPoint.getArgs())
                .filter(this::includeInFingerprint)
                .map(this::fingerprintValue)
                .toList();

        return RequestFingerprint.builder()
                .userId(userId)
                .httpMethod(request.getMethod())
                .endpoint(request.getRequestURI())
                .queryString(request.getQueryString())
                .requestBody(objectMapper.writeValueAsString(args))
                .build();
    }

    private boolean includeInFingerprint(Object argument) {
        return !(argument instanceof HttpServletRequest)
                && !(argument instanceof HttpServletResponse)
                && !(argument instanceof BindingResult)
                && !(argument instanceof Jwt)
                && !(argument instanceof Principal)
                && !(argument instanceof Pageable);
    }

    private Object fingerprintValue(Object argument) {
        if (argument instanceof MultipartFile file) {
            try {
                byte[] contentHash = MessageDigest.getInstance("SHA-256")
                        .digest(file.getBytes());
                return new FileFingerprint(
                        file.getOriginalFilename(),
                        file.getSize(),
                        file.getContentType(),
                        HexFormat.of().formatHex(contentHash)
                );
            } catch (IOException | NoSuchAlgorithmException exception) {
                throw new IllegalStateException(
                        "Unable to fingerprint uploaded file content.", exception
                );
            }
        }
        return argument;
    }

    private record FileFingerprint(
            String originalFilename,
            long size,
            String contentType,
            String contentSha256
    ) {
    }
}
