package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationMessage(
        String eventType,
        UUID userId,
        String email,
        String firstName,
        String verificationToken,
        String verificationUrl,
        Instant expiresAt
) {
}