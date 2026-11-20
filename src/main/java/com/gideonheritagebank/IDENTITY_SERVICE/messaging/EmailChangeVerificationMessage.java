package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import java.time.Instant;
import java.util.UUID;

public record EmailChangeVerificationMessage(
        String eventType,
        UUID userId,
        String oldEmail,
        String newEmail,
        String firstName,
        String verificationToken,
        String verificationUrl,
        Instant expiresAt
) {
}
