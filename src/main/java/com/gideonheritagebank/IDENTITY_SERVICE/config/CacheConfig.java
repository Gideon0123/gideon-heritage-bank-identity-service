package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {

        ObjectMapper redisObjectMapper = new ObjectMapper();

        // Support LocalDateTime, LocalDate, etc.
        redisObjectMapper.registerModule(new JavaTimeModule());

        /*
         * Only application classes are allowed to participate
         * in polymorphic deserialization.
         */
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.gideonheritagebank.IDENTITY_SERVICE")
                .build();

        /*
         * Let Spring Data Redis configure its own type resolver.
         *
         * This is important. Do NOT call:
         *
         * activateDefaultTyping(...)
         *
         * ourselves.
         */
        GenericJackson2JsonRedisSerializer serializer =
                GenericJackson2JsonRedisSerializer.builder()
                        .objectMapper(redisObjectMapper)
                        .defaultTyping(true)
                        .typeHintPropertyName("@class")
                        .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );
    }
}
