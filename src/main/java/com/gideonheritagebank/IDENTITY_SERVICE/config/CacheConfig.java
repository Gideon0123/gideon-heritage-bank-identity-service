package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gideonheritagebank.IDENTITY_SERVICE.cache.CacheNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        BasicPolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.gideonheritagebank.IDENTITY_SERVICE")
                        .allowIfSubType("java.util")
                        .allowIfSubType("java.time")
                        .build();

        redisObjectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "ghb:cache:" + cacheName + "::")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(valueSerializer)
                );
    }

    @Bean(name = "cacheManager")
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisCacheConfiguration redisCacheConfiguration
    ) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(CacheNames.CUSTOMER_PROFILE,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.CUSTOMER_ADDRESSES,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.CUSTOMER_STATUS,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(CacheNames.CUSTOMER_SEARCH,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(2)));

        cacheConfigurations.put(CacheNames.STAFF_PROFILE,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.STAFF_ADDRESSES,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.STAFF_EMPLOYMENT,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.STAFF_ROLE,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.STAFF_SEARCH,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(2)));

        cacheConfigurations.put(CacheNames.DEPARTMENTS,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
