package com.gideonheritagebank.IDENTITY_SERVICE.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange identityNotificationExchange(
            @Value("${identity.messaging.exchange:bank.notifications.exchange}")
            String exchangeName
    ) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue emailVerificationQueue(
            @Value("${identity.messaging.email-verification-queue:identity.email-verification}")
            String queueName
    ) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding emailVerificationBinding(
            Queue emailVerificationQueue,
            DirectExchange identityNotificationExchange,
            @Value("${identity.messaging.email-verification-routing-key:notification.email-verification}")
            String routingKey
    ) {
        return BindingBuilder
                .bind(emailVerificationQueue)
                .to(identityNotificationExchange)
                .with(routingKey);
    }
}