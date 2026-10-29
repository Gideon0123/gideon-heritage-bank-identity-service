package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.event.EmailVerificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${identity.messaging.exchange:bank.notifications.exchange}")
    private String exchange;

    @Value("${identity.messaging.email-verification-routing-key:notification.email-verification}")
    private String routingKey;

    public void publish(EmailVerificationRequestedEvent event) {
        EmailVerificationMessage payload = new EmailVerificationMessage(
                "CUSTOMER_EMAIL_VERIFICATION_REQUESTED",
                event.userId(),
                event.email(),
                event.firstName(),
                event.rawToken(),
                event.verificationUrl(),
                event.expiresAt()
        );

        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);

            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setContentEncoding(StandardCharsets.UTF_8.name());
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            properties.setMessageId(event.userId() + ":" + event.expiresAt());

            Message message = new Message(body, properties);

            rabbitTemplate.send(exchange, routingKey, message);

            log.info(
                    "Published customer email-verification request for user {}",
                    event.userId()
            );
        } catch (JsonProcessingException exception) {
            // Do not log the event or raw token.
            throw new IllegalStateException(
                    "Could not serialize the email-verification message",
                    exception
            );
        }
    }
}