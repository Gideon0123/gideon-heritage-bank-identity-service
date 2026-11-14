package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.IdempotencyRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class IdempotencyRedisConfig {

    @Bean
    public RedisTemplate<String, IdempotencyRecord> idempotencyRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, IdempotencyRecord> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<IdempotencyRecord> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, IdempotencyRecord.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;

    }

}