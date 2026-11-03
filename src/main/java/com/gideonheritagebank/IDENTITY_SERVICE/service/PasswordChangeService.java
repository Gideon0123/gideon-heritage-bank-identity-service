package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RefreshToken;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.AccountLockedException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.AuthenticationFailedException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final BankUserRepository bankUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${identity.security.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${identity.security.login.lock-duration:PT15M}")
    private Duration lockDuration;

    /*
     * ============================================================
     * CHANGE PASSWORD
     * ============================================================
     */

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public void changePassword(
            UUID userId,
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) {
        if (userId == null) {
            throw new BadRequest("An authenticated identity is required");
        }

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new BadRequest("Current password is required");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new BadRequest("New password is required");
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new BadRequest("Password confirmation is required");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequest(
                    "New password and confirmation password must match"
            );
        }

        if (newPassword.length() < 12 || newPassword.length() > 128) {
            throw new BadRequest(
                    "Password must contain between 12 and 128 characters"
            );
        }

        /*
         * Lock the user row to reduce concurrent credential-change races.
         */

        BankUser user = bankUserRepository.findByIdForUpdate(userId)
                .orElseThrow(AuthenticationFailedException::new);

        /*
         * A disabled or unverified identity cannot change its password
         * through this authenticated endpoint.
         *
         * Password recovery remains a separate workflow.
         */

        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new AccountLockedException(
                    "Password change is unavailable for this account"
            );
        }

        /*
         * Do not allow password changes through a session belonging
         * to an account that is currently locked.
         */

        if (!user.isAccountNonLocked()) {

            Instant lockedUntil = user.getLockedUntil();

            if (lockedUntil == null || lockedUntil.isAfter(Instant.now())) {
                throw new AccountLockedException(
                        "Your account is locked. Use password recovery "
                                + "or contact the bank if the lock is administrative."
                );
            }

            /*
             * An expired timed lock can be cleared.
             */

            user.setAccountNonLocked(true);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        /*
         * Verify the current password using the stored Argon2 hash.
         */

        boolean currentPasswordMatches = passwordEncoder.matches(
                currentPassword,
                user.getPasswordHash()
        );

        if (!currentPasswordMatches) {

            Instant now = Instant.now();
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            if (failedAttempts >= maxFailedAttempts) {

                user.setAccountNonLocked(false);
                user.setLockedUntil(now.plus(lockDuration));
            }

            bankUserRepository.saveAndFlush(user);

            /*
             * AuthenticationFailedException is excluded from rollback
             * so the failed-attempt count and any lockout are persisted.
             */

            throw new AuthenticationFailedException();
        }

         /* The new password must differ from the current password. */

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BadRequest(
                    "The new password must differ from the current password"
            );
        }

        /*
         * Encode the new password using the application's configured
         * PasswordEncoder. Never persist the raw password.
         */

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        /*
         * A successful credential change clears previous failed attempts.
         */

        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockedUntil(null);

        bankUserRepository.saveAndFlush(user);

         /* Revoke all active refresh tokens. */
        revokeAllActiveRefreshTokens(user, Instant.now());
    }


    private void revokeAllActiveRefreshTokens(
            BankUser user,
            Instant now
    ) {
        List<RefreshToken> refreshTokens =
                refreshTokenRepository.findAllByUser_Id(user.getId());

        boolean changed = false;

        for (RefreshToken refreshToken : refreshTokens) {

            if (refreshToken.getRevokedAt() == null) {

                refreshToken.setRevokedAt(now);
                changed = true;
            }
        }

        if (changed) {
            refreshTokenRepository.saveAllAndFlush(refreshTokens);
        }
    }
}