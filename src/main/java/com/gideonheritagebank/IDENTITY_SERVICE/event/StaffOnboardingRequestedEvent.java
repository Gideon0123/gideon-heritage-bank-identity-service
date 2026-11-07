package com.gideonheritagebank.IDENTITY_SERVICE.event;

import java.time.Instant;
import java.util.UUID;

public record StaffOnboardingRequestedEvent(
        UUID userId,
        String email,
        String firstName,
        String rawToken,
        String onboardingUrl,
        Instant expiresAt
) {
}
