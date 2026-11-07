package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import java.time.Instant;
import java.util.UUID;

public record StaffOnboardingMessage(
        String eventType,
        UUID userId,
        String email,
        String firstName,
        String onboardingToken,
        String onboardingUrl,
        Instant expiresAt
) {
}