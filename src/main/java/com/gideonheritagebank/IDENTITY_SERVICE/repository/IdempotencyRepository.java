package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.IdempotencyRecord;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.IdempotencyKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {

    private final RedisTemplate<String, IdempotencyRecord> redisTemplate;

    public Optional<IdempotencyRecord> find(
            Long userId,
            String key
    ) {
        String redisKey = IdempotencyKeyUtil.buildKey(userId, key);

        return Optional.ofNullable(
                redisTemplate.opsForValue().get(redisKey)
        );
    }

    public void save(
            Long userId,
            String key,
            IdempotencyRecord record,
            Duration ttl
    ) {
        String redisKey = IdempotencyKeyUtil.buildKey(userId, key);

        redisTemplate.opsForValue().set(redisKey, record, ttl);
    }

    public void delete(Long userId, String key) {
        redisTemplate.delete(IdempotencyKeyUtil.buildKey(userId, key));
    }

}