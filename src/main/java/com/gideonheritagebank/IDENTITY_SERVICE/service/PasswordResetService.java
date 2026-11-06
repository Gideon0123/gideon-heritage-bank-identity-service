package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.PasswordResetToken;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RefreshToken;
import com.gideonheritagebank.IDENTITY_SERVICE.event.PasswordResetRequestedEvent;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.PasswordResetTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.VerificationTokenExpired;

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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final BankUserRepository bankUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${identity.password-reset.token-ttl:PT15M}")
    private Duration tokenTtl;

    @Value("${identity.password-reset.frontend-url:http://localhost:3000/reset-password}")
    private String frontendResetUrl;

    /*
     * ============================================================
     * FORGOT PASSWORD
     * ============================================================
     */

    @Transactional
    public void requestPasswordReset(String suppliedEmail) {

        if (suppliedEmail == null || suppliedEmail.isBlank()) {
            return;
        }

        String email = suppliedEmail.trim().toLowerCase(Locale.ROOT);

        Optional<BankUser> userOptional = bankUserRepository.findByEmailForUpdate(email);

        if (userOptional.isEmpty()) {
            return;
        }

        BankUser user = userOptional.get();

        /*
         * Do not issue reset tokens for disabled or unverified identities.
         *
         * This check does not reveal the account state because the
         * controller always returns the same generic response.
         *
         * An account that is temporarily locked after failed logins
         * may still request a password reset.
         */

        if (user.isIdentityClosed() || !user.isEnabled() || !user.isEmailVerified()) {
            return;
        }

        Instant now = Instant.now();

        /*
         * Invalidate earlier outstanding reset tokens.
         *
         * The schema does not contain a separate revoked_at field for
         * password-reset tokens, so used_at also records invalidation.
         */

        List<PasswordResetToken> previousTokens =
                passwordResetTokenRepository.findAllByUser_Id(user.getId());

        for (PasswordResetToken previousToken : previousTokens) {
            if (previousToken.getUsedAt() == null) {
                previousToken.setUsedAt(now);
            }
        }

        if (!previousTokens.isEmpty()) {
            passwordResetTokenRepository.saveAllAndFlush(previousTokens);
        }

        /*
         * Generate a new unpredictable token.
         * Only the token's hash is persisted.
         */

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = now.plus(tokenTtl);

        PasswordResetToken resetToken = new PasswordResetToken(
                user,
                tokenHash,
                expiresAt
        );

        passwordResetTokenRepository.saveAndFlush(resetToken);

        String resetUrl = buildResetUrl(rawToken);

        /*
         * This event is dispatched to the transactional event listener.
         * The listener publishes to RabbitMQ only after this transaction
         * successfully commits.
         */

        eventPublisher.publishEvent(
                new PasswordResetRequestedEvent(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        rawToken,
                        resetUrl,
                        expiresAt
                )
        );

        // Never log the raw token or reset URL.
        log.info(
                "Password-reset request accepted for eligible userId={}",
                user.getId()
        );
    }

    /*
     * ============================================================
     * RESET PASSWORD
     * ============================================================
     */

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword,
            String confirmPassword
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequest("Invalid or expired password reset token");
        }

        if (newPassword == null || newPassword.isBlank() || confirmPassword == null
                || confirmPassword.isBlank() || !newPassword.equals(confirmPassword)) {

            throw new BadRequest("New password and confirmation password must match");
        }

        if (newPassword.length() < 12 || newPassword.length() > 128) {
            throw new BadRequest("Password must contain between 12 and 128 characters");
        }

        /*
         * Hash the supplied token before searching the database.
         * The raw token is never used as a database lookup value.
         */

        String submittedTokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashForUpdate(submittedTokenHash)
                .orElseThrow(() -> new BadRequest("Invalid or expired password reset token"));

        Instant now = Instant.now();

//        if (resetToken.getUsedAt() != null) {
//            throw new ConflictException(
//                    "This password reset token has already been used or invalidated"
//            );
//        }

        if (!resetToken.getExpiresAt().isAfter(now)) {
            throw new VerificationTokenExpired(
                    "Password reset token has expired"
            );
        }

        /*
         * Lock the user row as well.
         *
         * This reduces races with other operations that modify the
         * same identity, provided those operations use compatible locks.
         */

        UUID userId = resetToken.getUser().getId();

        BankUser user = bankUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BadRequest("Invalid or expired password reset token"));

        /*
         * Do not permit a reset for an identity that has been disabled
         * since the token was issued.
         */

        if (user.isIdentityClosed() || !user.isEnabled()) {
            throw new BadRequest("Password reset is not available for this account");
        }

        /*
         * Avoid accepting the same password again.
         */

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BadRequest("The new password must differ from the current password");
        }

        /*
         * Change the password.
         */
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        bankUserRepository.saveAndFlush(user);

        /*
         * Consume the submitted token.
         */

        resetToken.setUsedAt(now);
        passwordResetTokenRepository.saveAndFlush(resetToken);

        /*
         * Invalidate any other outstanding password-reset tokens.
         */

        List<PasswordResetToken> otherTokens =
                passwordResetTokenRepository.findAllByUser_Id(userId);

        for (PasswordResetToken otherToken : otherTokens) {
            if (!otherToken.getId().equals(resetToken.getId())
                    && otherToken.getUsedAt() == null) {

                otherToken.setUsedAt(now);
            }
        }

        if (!otherTokens.isEmpty()) {
            passwordResetTokenRepository.saveAllAndFlush(otherTokens);
        }

        /*
         * Revoke every active refresh-token session.
         *
         * The password change, token consumption, and session revocation
         * all occur in the same database transaction.
         */

        revokeAllActiveRefreshTokens(user, now);

        log.info(
                "Password reset completed for userId={}",
                userId
        );
    }

    /*
     * ============================================================
     * PRIVATE HELPERS
     * ============================================================
     */

    private String generateRawToken() {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            /*
             * SHA-256 is required by the Java platform.
             */

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private String buildResetUrl(String rawToken) {

        /*
         * UriComponentsBuilder safely encodes the token as a query
         * parameter and handles a URL that already contains a query.
         */

        return UriComponentsBuilder
                .fromUriString(frontendResetUrl)
                .queryParam("token", "{token}")
                .encode()
                .buildAndExpand(rawToken)
                .toUriString();
    }

    private void revokeAllActiveRefreshTokens(
            BankUser user,
            Instant now
    ) {

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser_Id(user.getId());

        boolean changed = false;

        for (RefreshToken token : tokens) {

            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                changed = true;
            }
        }

        if (changed) {
            refreshTokenRepository.saveAllAndFlush(tokens);
        }
    }
}