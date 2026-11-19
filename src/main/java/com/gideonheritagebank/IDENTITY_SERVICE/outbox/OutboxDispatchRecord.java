package com.gideonheritagebank.IDENTITY_SERVICE.outbox;

import java.util.UUID;

public record OutboxDispatchRecord(
        UUID id,
        String eventType,
        String exchangeName,
        String routingKey,
        String messageId,
        String encryptedPayload,
        String lockOwner,
        int attempts
) {
}
