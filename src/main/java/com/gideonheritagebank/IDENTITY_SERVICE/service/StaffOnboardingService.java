package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffOnboardingToken;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.VerificationTokenExpired;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffOnboardingTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffOnboardingService {

    private final BankUserRepository bankUserRepository;
    private final StaffOnboardingTokenRepository staffOnboardingTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void completeOnboarding(
            String rawToken,
            String newPassword,
            String confirmPassword
    ) {
        validateRequest(rawToken, newPassword, confirmPassword);

        String submittedTokenHash = hashToken(rawToken);

        StaffOnboardingToken onboardingToken = staffOnboardingTokenRepository
                .findByTokenHashForUpdate(submittedTokenHash)
                .orElseThrow(() -> new BadRequest(
                        "Invalid or expired staff onboarding token"
                ));

        Instant now = Instant.now();

        if (onboardingToken.isUsed() || onboardingToken.isRevoked()) {
            throw new BadRequest("Invalid or expired staff onboarding token");
        }

        if (onboardingToken.isExpired(now)) {
            throw new VerificationTokenExpired("Staff onboarding token has expired");
        }

        UUID userId = onboardingToken.getUser().getId();

        BankUser user = bankUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BadRequest("Invalid staff onboarding request"));

        /* The token must belong to a valid staff identity. */
        if (user.getStaffProfile() == null
                || user.getUserType() != com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType.STAFF) {

            throw new BadRequest("This onboarding token is not associated with a staff identity");
        }

        /* Do not allow onboarding to reopen a closed identity. */
        if (user.isIdentityClosed()) {
            throw new BadRequest("This identity has been closed");
        }

        /* A suspended identity must not be activated through onboarding. */
        if (user.isSuspended()) {
            throw new BadRequest("This staff identity is currently suspended");
        }

        /*
         * This endpoint is only for initial onboarding.
         * It must not become an alternative password-reset endpoint.
         */
        if (user.isEnabled()) {
            throw new BadRequest("This staff identity has already been activated");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BadRequest("The new password must differ from the existing password");
        }

        /*
         * Set the employee's own password.
         */
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        /*
         * Successful onboarding verifies the email address because
         * the invitation was delivered to the provisioned email address.
         */
        user.setEmailVerified(true);

        /*
         * Activate the identity.
         */
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        bankUserRepository.saveAndFlush(user);

        /* Consume this onboarding token. */
        onboardingToken.consume(now);

        staffOnboardingTokenRepository.saveAndFlush(onboardingToken);

        /* Revoke all other onboarding tokens for this employee. */
        revokeOtherOnboardingTokens(
                userId,
                onboardingToken.getId(),
                now
        );

        /*
         * Defensive session cleanup.
         * A newly provisioned identity should not normally have sessions,
         * but we ensure no previously issued refresh token survives.
         */
        revokeAllRefreshTokens(userId, now);

        log.info(
                "Staff onboarding completed successfully for userId={}",
                userId
        );
    }

    private void validateRequest(
            String rawToken,
            String newPassword,
            String confirmPassword
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequest("Invalid or expired staff onboarding token");
        }

        if (newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {

            throw new BadRequest("Password and password confirmation are required");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequest("Password and confirmation password must match");
        }

        if (newPassword.length() < 12 || newPassword.length() > 128) {
            throw new BadRequest("Password must contain between 12 and 128 characters");
        }
    }

    private void revokeOtherOnboardingTokens(
            UUID userId,
            UUID currentTokenId,
            Instant now
    ) {
        List<StaffOnboardingToken> tokens =
                staffOnboardingTokenRepository.findAllByUser_Id(userId);

        boolean changed = false;

        for (StaffOnboardingToken token : tokens) {

            if (!token.getId().equals(currentTokenId) && !token.isUsed() && !token.isRevoked()) {

                token.revoke(now);
                changed = true;
            }
        }

        if (changed) {
            staffOnboardingTokenRepository.saveAllAndFlush(tokens);
        }
    }

    private void revokeAllRefreshTokens(
            UUID userId,
            Instant now
    ) {
        var tokens = refreshTokenRepository.findAllByUser_Id(userId);

        boolean changed = false;

        for (var token : tokens) {

            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                changed = true;
            }
        }

        if (changed) {
            refreshTokenRepository.saveAllAndFlush(tokens);
        }
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }
}