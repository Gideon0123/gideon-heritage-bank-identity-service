package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.EmailVerificationToken;
import com.gideonheritagebank.IDENTITY_SERVICE.event.EmailVerificationRequestedEvent;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IllegalArgumentException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.VerificationTokenExpired;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final BankUserRepository bankUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${identity.verification.token-ttl:PT24H}")
    private Duration tokenTtl;

    @Value("${identity.verification.frontend-url:http://localhost:3000/verify-email}")
    private String verificationFrontendUrl;

    /**
     * Called from customer registration.
     * This joins the registration transaction. If registration rolls back,
     * the token and the after-commit event are rolled back/discarded too.
     */
    @Transactional
    public void createVerificationToken(BankUser user) {
        if (user.getUserType() == null || !user.getUserType().name().equals("CUSTOMER")) {
            throw new IllegalArgumentException(
                    "Email-verification tokens can only be issued to customers"
            );
        }

        if (user.isEmailVerified()) {
            return;
        }

        issueToken(user);
    }

    /**
     * Verifies a raw token supplied by the customer.
     */
    @Transactional
    public void verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequest("A verification token is required");
        }

        String tokenHash = hashToken(rawToken.trim());

        EmailVerificationToken token = tokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The verification token is invalid"
                ));

        if (token.getUsedAt() != null) {
            throw new BadRequest("The verification token has already been used");
        }

        Instant now = Instant.now();

        if (!token.getExpiresAt().isAfter(now)) {
            throw new VerificationTokenExpired(
                    "The verification token has expired. Request a new one."
            );
        }

        BankUser user = token.getUser();

        if (user.getUserType() == null
                || !user.getUserType().name().equals("CUSTOMER")) {
            throw new BadRequest(
                    "This token cannot verify the account"
            );
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);

            /*
             * Do not set enabled=true here.
             * Email verification must never activate a staff identity.
             * Customer login remains subject to the existing authentication
             * service's enabled and emailVerified checks.
             */
            bankUserRepository.save(user);
        }

        token.setUsedAt(now);
        tokenRepository.save(token);

        log.info("Email verified for user {}", user.getId());
    }

    /**
     * Requests a new verification token.
     * For privacy, the public controller should return the same response
     * whether or not the email exists.
     */
    @Transactional
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        BankUser user = bankUserRepository.findByEmailForUpdate(normalizedEmail).orElse(null);

        if (user == null || user.getUserType() == null
                || !user.getUserType().name().equals("CUSTOMER")
                || user.isEmailVerified()) {
            return;
        }

        issueToken(user);
    }

    private void issueToken(BankUser user) {
        if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalStateException(
                    "identity.verification.token-ttl must be a positive duration"
            );
        }

        /*
         * Invalidate previously issued, unused tokens for this user.
         * This prevents multiple outstanding verification links.
         */
        Instant now = Instant.now();

        for (EmailVerificationToken existing :
                tokenRepository.findAllByUser_Id(user.getId())) {

            if (existing.getUsedAt() == null) {
                existing.setUsedAt(now);
            }
        }
        tokenRepository.flush();

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = now.plus(tokenTtl);

        EmailVerificationToken verificationToken =
                new EmailVerificationToken(user, tokenHash, expiresAt);

        tokenRepository.saveAndFlush(verificationToken);

        String verificationUrl = buildVerificationUrl(rawToken);

        eventPublisher.publishEvent(new EmailVerificationRequestedEvent(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                rawToken,
                verificationUrl,
                expiresAt
        ));
    }

    private String buildVerificationUrl(String rawToken) {
        String baseUrl = verificationFrontendUrl.trim();

        if (baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "identity.verification.frontend-url must not be blank"
            );
        }

        String separator;

        if (baseUrl.contains("?")) {
            separator = baseUrl.endsWith("?") || baseUrl.endsWith("&")
                    ? "" : "&";
        } else {
            separator = "?";
        }

        // Base64 URL encoding uses URL-safe characters.
        return baseUrl + separator + "token=" + rawToken;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 is required by every compliant Java implementation.
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}