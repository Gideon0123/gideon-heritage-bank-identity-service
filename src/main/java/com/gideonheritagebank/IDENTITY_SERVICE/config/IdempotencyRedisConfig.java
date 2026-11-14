package com.gideonheritagebank.IDENTITY_SERVICE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class IdempotencyRedisConfig {

    /**
     * Idempotency records are stored as JSON strings. StringRedisTemplate makes
     * the Redis SET-NX operation and Lua compare-and-set scripts predictable.
     */
    @Bean
    public StringRedisTemplate idempotencyRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        return new StringRedisTemplate(connectionFactory);
    }
}
