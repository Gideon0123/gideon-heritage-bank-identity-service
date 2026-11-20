
package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.EmailChangeToken;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.VerificationTokenExpired;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.EmailChangeVerificationMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.messaging.EmailChangedConfirmedMessage;
import com.gideonheritagebank.IDENTITY_SERVICE.outbox.OutboxEventService;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.EmailChangeTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailChangeService {

    private static final long EMAIL_CHANGE_REQUEST_INTERVAL_MONTHS = 3;
    private static final long EMAIL_CHANGE_LOCK_DURATION_MONTHS = 6;
    private static final List<String> AVAILABLE_NOTIFICATION_CHANNELS =
            List.of(
                    "EMAIL",
                    "PUSH",
                    "IN_APP",
                    "WEBSOCKET"
            );

    private final BankUserRepository bankUserRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OutboxEventService outboxEventService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${identity.email-change.token-ttl:PT24H}")
    private Duration tokenTtl;

    @Value("${identity.email-change.frontend-url:http://localhost:3000/verify-email-change}")
    private String verificationFrontendUrl;

    /* REQUEST EMAIL CHANGE */
    @Transactional
    public void requestEmailChange(
            UUID userId,
            String requestedEmail
    ) {
        if (userId == null) {
            throw new BadRequest("The authenticated identity could not be identified.");
        }

        String newEmail = normalizeEmail(requestedEmail);

        if (newEmail.isBlank()) {
            throw new BadRequest("A new email address is required.");
        }

        BankUser user = bankUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity not found."));

        Instant now = Instant.now();
        validateIdentityCanChangeEmail(user);
        String oldEmail = normalizeEmail(user.getEmail());

        if (oldEmail.equals(newEmail)) {
            throw new ConflictException(
                    "The new email address is already associated with this identity."
            );
        }

        /*
         * Check whether another identity already owns this address.
         * The database unique constraint remains the final protection
         * against concurrent requests.
         */
        if (bankUserRepository.existsByEmail(newEmail)) {
            throw new ConflictException("This email address is already registered.");
        }

        validateRequestInterval(user, now);

        validateSuccessfulChangeLock(user, now);

        /*
         * Revoke previous outstanding email-change tokens.
         * Only one active email-change verification token should exist.
         */
        revokeOutstandingTokens(userId, now);

        /*
         * Update the stored email immediately, as requested.
         *
         * The account becomes unverified until the new address
         * is successfully verified.
         */
        user.setEmail(newEmail);
        user.setEmailVerified(false);
        user.setLastEmailChangeRequestedAt(now);

        try {
            bankUserRepository.saveAndFlush(user);

        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "This email address is already registered."
            );
        }

        /*
         * Revoke all existing refresh tokens.
         * The old refresh-token sessions must not be renewable
         * while the identity's email is unverified.
         */
        revokeRefreshTokens(userId, now);

        /* Generate a cryptographically random one-time token. */
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = now.plus(tokenTtl);

        EmailChangeToken emailChangeToken = new EmailChangeToken(
                user,
                oldEmail,
                newEmail,
                tokenHash,
                expiresAt
        );

        emailChangeTokenRepository.saveAndFlush(emailChangeToken);

        String verificationUrl = buildVerificationUrl(rawToken);

        /*
         * This outbox insertion joins this transaction.
         * If this transaction rolls back:
         * - the email update rolls back;
         * - emailVerified rolls back;
         * - the token rolls back;
         * - the outbox event rolls back.
         */
        outboxEventService.enqueueEmailChangeVerification(
                new EmailChangeVerificationMessage(
                        "EMAIL_CHANGE_VERIFICATION_REQUESTED",
                        user.getId(),
                        oldEmail,
                        newEmail,
                        user.getFirstName(),
                        rawToken,
                        verificationUrl,
                        expiresAt
                )
        );

        log.info(
                "Email change requested for userId={}",
                userId
        );
    }

    /* VERIFY NEW EMAIL */
    @Transactional
    public void verifyEmailChange(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequest("An email change verification token is required.");
        }

        String tokenHash = hashToken(rawToken.trim());

        EmailChangeToken token = emailChangeTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadRequest(
                        "The email change verification token is invalid."
                ));

        if (token.isUsed()) {
            throw new ConflictException("This email change verification token has already been used.");
        }

        if (token.isRevoked()) {
            throw new ConflictException("This email change verification token is no longer valid.");
        }

        Instant now = Instant.now();

        if (!token.getExpiresAt().isAfter(now)) {
            throw new VerificationTokenExpired("The email change verification token has expired.");
        }

        BankUser user = token.getUser();

        if (user.isIdentityClosed()) {
            throw new BadRequest("This identity is closed and cannot verify an email change.");
        }

        if (user.isSuspended()) {
            throw new BadRequest("A suspended identity cannot complete an email change.");
        }

        /*
         * Ensure the token is still associated with the email
         * currently stored on the identity.
         * This prevents an older token from verifying an address
         * after a later change request.
         */
        if (!normalizeEmail(user.getEmail()).equals(token.getNewEmail())) {
            throw new ConflictException(
                    "This verification token does not match the current email address."
            );
        }

        if (user.isEmailVerified()) {
            throw new ConflictException("The identity's email address is already verified.");
        }

        /* Verify the new email address. */
        user.setEmailVerified(true);

        /*
         * Start the six-month restriction only after successful
         * ownership verification.
         */
        user.setEmailChangedAt(now);
        Instant nextAllowedChangeAt = now.atZone(java.time.ZoneOffset.UTC)
                .plusMonths(EMAIL_CHANGE_LOCK_DURATION_MONTHS)
                .toInstant();
        user.setEmailChangeLockedUntil(nextAllowedChangeAt);
        token.markUsed(now);

        bankUserRepository.saveAndFlush(user);
        emailChangeTokenRepository.save(token);

        /*
         * Publish the confirmation event in the same transaction.
         * Notification Service can use the user's preferences
         * to decide which channels are enabled.
         */
        outboxEventService.enqueueEmailChangedConfirmed(
                new EmailChangedConfirmedMessage(
                        "EMAIL_CHANGE_CONFIRMED",
                        user.getId(),
                        token.getOldEmail(),
                        token.getNewEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        now,
                        nextAllowedChangeAt,
                        "USER_PREFERENCE",
                        AVAILABLE_NOTIFICATION_CHANNELS
                )
        );

        log.info(
                "Email change verified successfully for userId={}",
                user.getId()
        );
    }

    /* VALIDATION */
    private void validateIdentityCanChangeEmail(BankUser user) {

        if (user.isIdentityClosed()) {
            throw new ConflictException("A closed identity cannot change its email address.");
        }

        if (user.isSuspended()) {
            throw new ConflictException("A suspended identity cannot change its email address.");
        }

        if (!user.isEnabled()) {
            throw new ConflictException("A disabled identity cannot change its email address.");
        }

        /*
         * An identity must have a verified current email before
         * initiating another email change.
         */
        if (!user.isEmailVerified()) {
            throw new ConflictException(
                    "Your current email address must be verified before you can change it."
            );
        }
    }

    private void validateRequestInterval(
            BankUser user,
            Instant now
    ) {

        Instant lastRequest = user.getLastEmailChangeRequestedAt();

        if (lastRequest == null) {
            return;
        }

        Instant nextRequestAllowedAt = lastRequest.atZone(java.time.ZoneOffset.UTC)
                .plusMonths(EMAIL_CHANGE_REQUEST_INTERVAL_MONTHS)
                .toInstant();

        if (now.isBefore(nextRequestAllowedAt)) {
            throw new ConflictException(
                    "You can only request an email change once every three months. "
                            + "You can request another change after "
                            + nextRequestAllowedAt + "."
            );
        }
    }

    private void validateSuccessfulChangeLock(
            BankUser user,
            Instant now
    ) {

        Instant lockedUntil = user.getEmailChangeLockedUntil();

        if (lockedUntil != null && now.isBefore(lockedUntil)) {
            throw new ConflictException(
                    "Your email was recently changed. "
                            + "You can change it again after "
                            + lockedUntil + "."
            );
        }
    }

    /* TOKEN MANAGEMENT */
    private void revokeOutstandingTokens(
            UUID userId,
            Instant now
    ) {
        List<EmailChangeToken> tokens = emailChangeTokenRepository.findAllByUser_Id(userId);

        for (EmailChangeToken token : tokens) {

            if (!token.isUsed() && !token.isRevoked()) {
                token.revoke(now);
            }
        }

        emailChangeTokenRepository.saveAllAndFlush(tokens);
    }

    private void revokeRefreshTokens(
            UUID userId,
            Instant now
    ) {

        refreshTokenRepository.findAllByUser_Id(userId)
                .stream()
                .filter(token -> token.getRevokedAt() == null)
                .forEach(token -> token.setRevokedAt(now));
    }

    /* CRYPTOGRAPHIC HELPERS */
    private String generateRawToken() {

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    /* EMAIL HELPERS */
    private String normalizeEmail(String email) {

        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildVerificationUrl(String rawToken) {

        if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalStateException(
                    "identity.email-change.token-ttl must be positive."
            );
        }

        String baseUrl = verificationFrontendUrl.trim();

        if (baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "identity.email-change.frontend-url must not be blank."
            );
        }

        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("token", "{token}")
                .encode()
                .buildAndExpand(rawToken)
                .toUriString();
    }
}
