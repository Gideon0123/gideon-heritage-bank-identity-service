package com.gideonheritagebank.IDENTITY_SERVICE.messaging;

import com.gideonheritagebank.IDENTITY_SERVICE.event.PasswordResetRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEventListener {

    private final PasswordResetMessagePublisher publisher;

    @Async("identityEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequested(
            PasswordResetRequestedEvent event
    ) {
        try {
            publisher.publish(event);

        } catch (Exception exception) {

            /*
             * Do not log the token, reset URL, or message body.
             *
             * This is temporary best-effort delivery. If publication
             * fails, the reset-token record may still exist even though
             * the notification was not delivered.
             *
             * Stage 5 will address durable publication using an outbox,
             * retries, and appropriate failure monitoring.
             */

            log.error(
                    "Password-reset notification publication failed for userId={}",
                    event.userId()
            );
        }
    }
}