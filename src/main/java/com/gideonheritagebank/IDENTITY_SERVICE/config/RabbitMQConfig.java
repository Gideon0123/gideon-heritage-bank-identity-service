package com.gideonheritagebank.IDENTITY_SERVICE.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /**
     * Declares the direct exchange used by the Identity Service
     * to publish customer email-verification messages.
     */
    @Bean
    public DirectExchange identityNotificationExchange(
            @Value("${identity.messaging.exchange:bank.notifications.exchange}")
            String exchangeName
    ) {
        return new DirectExchange(
                exchangeName,
                true,
                false
        );
    }

    /**
     * Declares the durable email-verification queue.
     */
    @Bean
    public Queue emailVerificationQueue(
            @Value("${identity.messaging.email-verification-queue:identity.email-verification}")
            String queueName
    ) {
        return QueueBuilder
                .durable(queueName)
                .build();
    }

    /**
     * Binds the verification queue to the notification exchange
     * using the configured routing key.
     */
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

    /**
     * Explicitly provides RabbitAdmin.
     * RabbitAdmin uses the application's RabbitMQ ConnectionFactory
     * to declare exchanges, queues and bindings.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(
            ConnectionFactory connectionFactory
    ) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;
    }

    /**
     * Requests declaration of the configured RabbitMQ resources
     * after the Spring application context has started.
     */
    @Bean
    public ApplicationRunner declareRabbitMQResourcesOnStartup(
            AmqpAdmin amqpAdmin
    ) {
        return args -> amqpAdmin.initialize();
    }
}