package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.event.StaffOnboardingRequestedEvent;
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
public class StaffOnboardingMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${identity.messaging.exchange:bank.notifications.exchange}")
    private String exchangeName;

    @Value("${identity.messaging.staff-onboarding-routing-key:notification.staff-onboarding}")
    private String routingKey;

    public void publish(StaffOnboardingRequestedEvent event) {

        StaffOnboardingMessage payload = new StaffOnboardingMessage(
                "STAFF_ONBOARDING_REQUESTED",
                event.userId(),
                event.email(),
                event.firstName(),
                event.rawToken(),
                event.onboardingUrl(),
                event.expiresAt()
        );

        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);

            MessageProperties properties = new MessageProperties();

            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setContentEncoding(StandardCharsets.UTF_8.name());
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            properties.setMessageId(
                    event.userId() + ":STAFF_ONBOARDING:" + event.expiresAt()
            );

            Message message = new Message(body, properties);

            rabbitTemplate.send(
                    exchangeName,
                    routingKey,
                    message
            );

            log.info(
                    "Staff onboarding notification submitted for userId={}",
                    event.userId()
            );

        } catch (JsonProcessingException exception) {

            log.error(
                    "Could not serialize staff onboarding notification for userId={}",
                    event.userId()
            );

            throw new IllegalStateException(
                    "Could not serialize staff onboarding notification",
                    exception
            );
        }
    }
}