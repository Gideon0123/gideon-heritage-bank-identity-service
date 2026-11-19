package com.gideonheritagebank.IDENTITY_SERVICE.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private final OutboxClaimService claimService;
    private final OutboxEventService outboxEventService;
    private final OutboxPayloadEncryptionService encryptionService;
    private final RabbitTemplate outboxRabbitTemplate;

    @Value("${identity.outbox.relay.confirm-timeout:PT10S}")
    private Duration confirmTimeout;

    @Scheduled(fixedDelayString = "${identity.outbox.relay.fixed-delay:1000}")
    public void relayPendingEvents() {

        List<OutboxDispatchRecord> batch = claimService.claimBatch();

        for (OutboxDispatchRecord record : batch) {

            publishOne(record);
        }
    }

    private void publishOne(
            OutboxDispatchRecord record
    ) {

        try {
            byte[] body = encryptionService.decrypt(
                    record.encryptedPayload()
            );

            MessageProperties properties = new MessageProperties();

            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setContentEncoding(StandardCharsets.UTF_8.name());
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            /* Preserve the existing message ID format. */
            properties.setMessageId(record.messageId());

            properties.setHeader(
                    "eventType",
                    record.eventType()
            );

            Message message = new Message(body, properties);

            CorrelationData correlationData = new CorrelationData(
                    record.id().toString()
            );

            outboxRabbitTemplate.send(
                    record.exchangeName(),
                    record.routingKey(),
                    message,
                    correlationData
            );

            /* Wait for RabbitMQ's publisher confirmation. */
            CorrelationData.Confirm confirm = correlationData.getFuture().get(
                    confirmTimeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!confirm.isAck()) {

                throw new AmqpException("RabbitMQ negatively acknowledged the publication");
            }

            /*
             * An ACK does not mean the message was routed.
             * A returned message means the exchange could not route it.
             */
            if (correlationData.getReturned() != null) {

                throw new AmqpException("RabbitMQ returned the publication as unroutable");
            }

            outboxEventService.markPublished(
                    record.id(),
                    record.lockOwner()
            );

            log.info(
                    "Outbox event published: eventId={}, eventType={}, attempt={}",
                    record.id(),
                    record.eventType(),
                    record.attempts()
            );

        } catch (Exception exception) {

            try {
                outboxEventService.recordFailure(
                        record,
                        exception
                );

            } catch (Exception stateException) {

                /*
                 * If the lease expired and another worker claimed
                 * the row, this worker must not overwrite its state.
                 * The next relay cycle can recover the record.
                 */
                log.error(
                        "Could not update outbox state after publication failure: eventId={}, eventType={}",
                        record.id(),
                        record.eventType()
                );
            }

            /* Do not log exception messages or the message body. */
            log.warn(
                    "Outbox publication attempt failed: eventId={}, eventType={}, attempt={}, failureType={}",
                    record.id(),
                    record.eventType(),
                    record.attempts(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
