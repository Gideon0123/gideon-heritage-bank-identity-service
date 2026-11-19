package com.gideonheritagebank.IDENTITY_SERVICE.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OutboxMessagingConfig {

    @Bean
    public RabbitTemplate outboxRabbitTemplate(
            ConnectionFactory connectionFactory
    ) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setReturnsCallback(returned -> {

            /* Never log the returned message body. */
            log.error(
                    "RabbitMQ returned an unroutable outbox message: exchange={}, routingKey={}, replyCode={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode()
            );
        });

        return rabbitTemplate;
    }
}