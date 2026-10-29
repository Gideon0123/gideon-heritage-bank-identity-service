package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import com.gideonheritagebank.IDENTITY_SERVICE.event.EmailVerificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationEventListener {

    private final EmailVerificationMessagePublisher publisher;

    @Async("identityEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EmailVerificationRequestedEvent event) {
        try {
            publisher.publish(event);
        } catch (Exception exception) {
            // Never log the token or the complete event.
            log.error(
                    "Could not publish email-verification request for user {}. " +
                            "The user can request a new verification email.",
                    event.userId(),
                    exception
            );
        }
    }
}
