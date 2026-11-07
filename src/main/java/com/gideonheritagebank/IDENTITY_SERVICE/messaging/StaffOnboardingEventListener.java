package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import com.gideonheritagebank.IDENTITY_SERVICE.event.StaffOnboardingRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffOnboardingEventListener {

    private final StaffOnboardingMessagePublisher publisher;

    @Async("identityEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStaffOnboardingRequested(
            StaffOnboardingRequestedEvent event
    ) {
        try {

            publisher.publish(event);

        } catch (Exception exception) {

            /*
             * Never log the raw token or onboarding URL.
             *
             * This follows the same best-effort publication approach
             * currently used by PasswordResetEventListener.
             *
             * A durable outbox and retry mechanism can be introduced
             * in the later messaging reliability stage.
             */

            log.error(
                    "Staff onboarding notification publication failed for userId={}",
                    event.userId()
            );
        }
    }
}