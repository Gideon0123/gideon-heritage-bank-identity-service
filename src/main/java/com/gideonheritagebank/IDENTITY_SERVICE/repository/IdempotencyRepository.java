package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.IdempotencyRecord;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.IdempotencyKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {

    private static final DefaultRedisScript<Long> COMPLETE_IF_OWNER_SCRIPT =
            new DefaultRedisScript<>("""
                    local current = redis.call('GET', KEYS[1])
                    if not current then return 0 end
                    local ok, decoded = pcall(cjson.decode, current)
                    if not ok or decoded['processingToken'] ~= ARGV[1] then return 0 end
                    redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                    return 1
                    """, Long.class);

    private static final DefaultRedisScript<Long> DELETE_IF_OWNER_SCRIPT =
            new DefaultRedisScript<>("""
                    local current = redis.call('GET', KEYS[1])
                    if not current then return 0 end
                    local ok, decoded = pcall(cjson.decode, current)
                    if not ok or decoded['processingToken'] ~= ARGV[1] then return 0 end
                    return redis.call('DEL', KEYS[1])
                    """, Long.class);

    private final StringRedisTemplate idempotencyRedisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<IdempotencyRecord> find(UUID userId, String key) {
        String redisKey = IdempotencyKeyUtil.buildKey(userId, key);
        String json = idempotencyRedisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, IdempotencyRecord.class));
        } catch (JsonProcessingException exception) {
            // A malformed entry must not permanently block the client key.
            idempotencyRedisTemplate.delete(redisKey);
            throw new IllegalStateException("Unable to read idempotency record from Redis.", exception);
        }
    }

    /** Atomic Redis SET key value NX PX ttl. Only one concurrent request wins. */
    public boolean createIfAbsent(
            UUID userId,
            String key,
            IdempotencyRecord record,
            Duration ttl
    ) {
        validateTtl(ttl);
        String redisKey = IdempotencyKeyUtil.buildKey(userId, key);
        String json = toJson(record);
        Boolean created = idempotencyRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, json, ttl);
        return Boolean.TRUE.equals(created);
    }

    /**
     * Atomically replaces PROCESSING with COMPLETED only if this request still
     * owns the Redis entry. A slow request cannot overwrite a newer retry.
     */
    public boolean completeIfOwner(
            UUID userId,
            String key,
            UUID processingToken,
            IdempotencyRecord completedRecord,
            Duration ttl
    ) {
        validateTtl(ttl);
        String redisKey = IdempotencyKeyUtil.buildKey(userId, key);
        Long result = idempotencyRedisTemplate.execute(
                COMPLETE_IF_OWNER_SCRIPT,
                List.of(redisKey),
                processingToken.toString(),
                toJson(completedRecord),
                Long.toString(ttl.toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    public void deleteIfOwner(UUID userId, String key, UUID processingToken) {
        String redisKey = IdempotencyKeyUtil.buildKey(userId, key);
        idempotencyRedisTemplate.execute(
                DELETE_IF_OWNER_SCRIPT,
                List.of(redisKey),
                processingToken.toString()
        );
    }

    private String toJson(IdempotencyRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize idempotency record.", exception);
        }
    }

    private void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Idempotency TTL must be greater than zero.");
        }
    }
}
