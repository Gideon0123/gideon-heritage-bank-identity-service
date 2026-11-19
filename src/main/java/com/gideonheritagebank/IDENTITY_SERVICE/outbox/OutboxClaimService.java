package com.gideonheritagebank.IDENTITY_SERVICE.outbox;

import com.gideonheritagebank.IDENTITY_SERVICE.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${identity.outbox.relay.batch-size:25}")
    private int batchSize;

    @Value("${identity.outbox.relay.lease-duration:PT2M}")
    private Duration leaseDuration;

    @Transactional
    public List<OutboxDispatchRecord> claimBatch() {

        Instant now = Instant.now();

        String owner = UUID.randomUUID().toString();

        List<OutboxEvent> events = outboxEventRepository.findBatchForRelay(
                now,
                batchSize
        );

        for (OutboxEvent event : events) {

            event.claim(
                    owner,
                    now.plus(leaseDuration),
                    now
            );
        }

        return events.stream()
                .map(event -> new OutboxDispatchRecord(
                        event.getId(),
                        event.getEventType(),
                        event.getExchangeName(),
                        event.getRoutingKey(),
                        event.getMessageId(),
                        event.getEncryptedPayload(),
                        owner,
                        event.getAttempts()
                ))
                .toList();
    }
}