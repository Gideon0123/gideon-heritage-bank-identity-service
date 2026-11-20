package com.gideonheritagebank.IDENTITY_SERVICE.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.CustomerConvertedToStaffMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.EmailChangeVerificationMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.EmailChangedConfirmedMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.EmailVerificationMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.PasswordResetMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.StaffOnboardingMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    @Value("${identity.messaging.exchange:bank.notifications.exchange}")
    private String exchangeName;

    @Value("${identity.messaging.email-verification-routing-key:notification.email-verification}")
    private String emailVerificationRoutingKey;

    @Value("${identity.messaging.password-reset-routing-key:notification.password-reset}")
    private String passwordResetRoutingKey;

    @Value("${identity.messaging.staff-onboarding-routing-key:notification.staff-onboarding}")
    private String staffOnboardingRoutingKey;

    @Value("${identity.messaging.customer-converted-routing-key:notification.customer-converted-to-staff}")
    private String customerConvertedRoutingKey;

    @Value("${identity.messaging.email-change-verification-routing-key:notification.email-change-verification}")
    private String emailChangeVerificationRoutingKey;

    @Value("${identity.messaging.email-changed-routing-key:notification.email-changed}")
    private String emailChangedRoutingKey;

    @Value("${identity.outbox.retry.max-attempts:12}")
    private int maxAttempts;

    @Value("${identity.outbox.retry.initial-delay:PT5S}")
    private Duration initialRetryDelay;

    @Value("${identity.outbox.retry.max-delay:PT30M}")
    private Duration maximumRetryDelay;

    /* EXISTING EVENTS */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueEmailVerification(
            EmailVerificationMessage message
    ) {
        String messageId = message.userId() + ":" + message.expiresAt();

        enqueue(
                "CUSTOMER_EMAIL_VERIFICATION_REQUESTED",
                emailVerificationRoutingKey,
                messageId,
                message
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueuePasswordReset(
            PasswordResetMessage message
    ) {
        String messageId = message.userId() + ":" + message.expiresAt();

        enqueue(
                "PASSWORD_RESET_REQUESTED",
                passwordResetRoutingKey,
                messageId,
                message
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueStaffOnboarding(
            StaffOnboardingMessage message
    ) {
        String messageId = message.userId()
                + ":STAFF_ONBOARDING:"
                + message.expiresAt();

        enqueue(
                "STAFF_ONBOARDING_REQUESTED",
                staffOnboardingRoutingKey,
                messageId,
                message
        );
    }

    /* NEW EVENT 1: CUSTOMER CONVERTED TO STAFF */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueCustomerConvertedToStaff(
            CustomerConvertedToStaffMessage message
    ) {
        String messageId = message.userId()
                + ":CUSTOMER_CONVERTED_TO_STAFF:"
                + message.convertedAt();

        enqueue(
                "CUSTOMER_CONVERTED_TO_STAFF",
                customerConvertedRoutingKey,
                messageId,
                message
        );
    }

    /* NEW EVENT 2: EMAIL CHANGE VERIFICATION */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueEmailChangeVerification(
            EmailChangeVerificationMessage message
    ) {
        String messageId = message.userId()
                + ":EMAIL_CHANGE_VERIFICATION:"
                + message.expiresAt();

        enqueue(
                "EMAIL_CHANGE_VERIFICATION_REQUESTED",
                emailChangeVerificationRoutingKey,
                messageId,
                message
        );
    }

    /* NEW EVENT 3: EMAIL CHANGE CONFIRMED */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueEmailChangedConfirmed(
            EmailChangedConfirmedMessage message
    ) {
        String messageId = message.userId()
                + ":EMAIL_CHANGED:"
                + message.emailChangedAt();

        enqueue(
                "EMAIL_CHANGE_CONFIRMED",
                emailChangedRoutingKey,
                messageId,
                message
        );
    }

    /* COMMON OUTBOX INSERTION */
    private void enqueue(
            String eventType,
            String routingKey,
            String messageId,
            Object payload
    ) {
        try {
            byte[] jsonPayload = objectMapper.writeValueAsBytes(payload);

            String encryptedPayload = encryptionService.encrypt(jsonPayload);

            OutboxEvent event = OutboxEvent.pending(
                    eventType,
                    exchangeName,
                    routingKey,
                    messageId,
                    encryptedPayload,
                    Instant.now()
            );

            outboxEventRepository.save(event);

        } catch (JsonProcessingException exception) {

            /*
             * Do not log the payload. Some events contain
             * one-time credentials and verification URLs.
             */
            throw new IllegalStateException(
                    "Could not serialize outbox event",
                    exception
            );
        }
    }

    /* OUTBOX PUBLICATION SUCCESS */
    @Transactional
    public void markPublished(
            UUID eventId,
            String lockOwner
    ) {
        OutboxEvent event = getRequiredEvent(eventId);

        event.markPublished(
                lockOwner,
                Instant.now()
        );
    }

    /*
     * ============================================================
     * OUTBOX PUBLICATION FAILURE
     * ============================================================
     */

    @Transactional
    public void recordFailure(
            OutboxDispatchRecord record,
            Exception exception
    ) {
        OutboxEvent event = getRequiredEvent(record.id());

        /*
         * Store only the exception type.
         * Do not persist exception messages that could contain
         * sensitive payload information.
         */
        String safeErrorCode = exception.getClass().getSimpleName();

        Instant now = Instant.now();

        if (event.getAttempts() >= maxAttempts) {

            event.markFailed(
                    record.lockOwner(),
                    safeErrorCode,
                    now
            );

            return;
        }

        Duration delay = calculateRetryDelay(event.getAttempts());

        event.scheduleRetry(
                record.lockOwner(),
                now.plus(delay),
                safeErrorCode,
                now
        );
    }

    private Duration calculateRetryDelay(int attempt) {

        long multiplier = 1L << Math.clamp(attempt - 1, 0, 20);

        Duration calculated;

        try {
            calculated = initialRetryDelay.multipliedBy(multiplier);

        } catch (ArithmeticException exception) {
            calculated = maximumRetryDelay;
        }

        return calculated.compareTo(maximumRetryDelay) > 0
                ? maximumRetryDelay : calculated;
    }

    private OutboxEvent getRequiredEvent(UUID eventId) {

        return outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event does not exist: " + eventId
                ));
    }
}