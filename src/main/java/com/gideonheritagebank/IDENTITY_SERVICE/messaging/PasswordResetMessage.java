package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetMessage(
        String eventType,
        UUID userId,
        String email,
        String firstName,
        String resetToken,
        String resetUrl,
        Instant expiresAt
) {
}