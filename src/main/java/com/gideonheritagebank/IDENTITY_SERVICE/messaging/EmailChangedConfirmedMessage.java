package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmailChangedConfirmedMessage(
        String eventType,
        UUID userId,
        String oldEmail,
        String newEmail,
        String firstName,
        String lastName,
        Instant emailChangedAt,
        Instant nextEmailChangeAllowedAt,
        String deliveryPolicy,
        List<String> availableChannels
) {
}
