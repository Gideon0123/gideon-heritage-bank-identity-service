package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.event.PasswordResetRequestedEvent;
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
public class PasswordResetMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${identity.messaging.exchange:bank.notifications.exchange}")
    private String exchangeName;

    @Value("${identity.messaging.password-reset-routing-key:notification.password-reset}")
    private String routingKey;

    public void publish(PasswordResetRequestedEvent event) {

        PasswordResetMessage payload = new PasswordResetMessage(
                "PASSWORD_RESET_REQUESTED",
                event.userId(),
                event.email(),
                event.firstName(),
                event.rawToken(),
                event.resetUrl(),
                event.expiresAt()
        );

        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);

            MessageProperties properties = new MessageProperties();

            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setContentEncoding(StandardCharsets.UTF_8.name());
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            // A stable identifier for this particular reset request.
            properties.setMessageId(
                    event.userId() + ":" + event.expiresAt()
            );

            Message message = new Message(body, properties);

            rabbitTemplate.send(
                    exchangeName,
                    routingKey,
                    message
            );

            // Never log the token, reset URL, or message payload.
            log.info(
                    "Password-reset notification submitted for userId={}",
                    event.userId()
            );

        } catch (JsonProcessingException exception) {

            // Never include the serialized payload in the exception log.
            log.error(
                    "Could not serialize password-reset notification for userId={}",
                    event.userId()
            );

            throw new IllegalStateException(
                    "Could not serialize password-reset notification",
                    exception
            );
        }
    }
}
