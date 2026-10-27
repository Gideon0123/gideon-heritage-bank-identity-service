package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.internal.AuthenticationResult;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.LoginRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RefreshToken;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.AuthenticationFailedException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.security.JwtService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final BankUserRepository bankUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IdentityMapper identityMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${identity.security.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${identity.security.login.lock-duration:PT15M}")
    private Duration lockDuration;

    @Value("${identity.jwt.refresh-token-ttl:P30D}")
    private Duration refreshTokenTtl;

    private String dummyPasswordHash;

    /*
     * A dummy Argon2 hash helps reduce the obvious timing difference
     * between an unknown email address and an incorrect password.
     *
     * This is a supplementary mitigation, not a complete defence
     * against account enumeration.
     */
    @PostConstruct
    void initializeDummyPasswordHash() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String dummyPassword = Base64.getEncoder().encodeToString(randomBytes);
        dummyPasswordHash = passwordEncoder.encode(dummyPassword);
    }

    /*
     * ============================================================
     * LOGIN
     * ============================================================
     */

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthenticationResult login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        String rawPassword = request.getPassword();

        Optional<BankUser> userOptional = bankUserRepository.findByEmailForUpdate(email);

        /*
         * Unknown email:
         * perform a dummy Argon2 comparison and return a generic error.
         */
        if (userOptional.isEmpty()) {

            passwordEncoder.matches(
                    rawPassword,
                    dummyPasswordHash
            );

            throw new AuthenticationFailedException();
        }

        BankUser user = userOptional.get();

        /*
         * Verify the submitted password.
         */
        boolean passwordMatches = passwordEncoder.matches(
                rawPassword,
                user.getPasswordHash()
        );

        Instant now = Instant.now();

        /*
         * Check whether the account is locked.
         *
         * A lock with a future expiry remains active.
         * A lock with no expiry is treated as an administrative lock.
         * An expired timed lock can be cleared automatically.
         */
        if (!user.isAccountNonLocked()) {

            Instant lockedUntil = user.getLockedUntil();

            if (lockedUntil != null && !lockedUntil.isAfter(now)) {

                user.setAccountNonLocked(true);
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);

            } else {

                throw new AuthenticationFailedException();
            }
        }

        /*
         * Do not authenticate disabled or unverified accounts.
         */
        if (!user.isEnabled() || !user.isEmailVerified()) {

            throw new AuthenticationFailedException();
        }

        /*
         * Record an invalid password.
         */
        if (!passwordMatches) {

            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            /*
             * Apply the timed lock when the configured threshold
             * is reached.
             */
            if (failedAttempts >= maxFailedAttempts) {

                user.setAccountNonLocked(false);
                user.setLockedUntil(now.plus(lockDuration));
            }

            bankUserRepository.saveAndFlush(user);

            /*
             * The transaction must commit the failed-attempt counter
             * and any lockout changes despite this exception.
             */
            throw new AuthenticationFailedException();
        }

        /*
         * Successful authentication:
         * reset the failed-login counter and stale lock metadata.
         */
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockedUntil(null);

        BankUser authenticatedUser = bankUserRepository.saveAndFlush(user);

        /*
         * Issue the signed, short-lived access token.
         */
        String accessToken =
                jwtService.generateAccessToken(authenticatedUser);

        /*
         * Generate a cryptographically random refresh token.
         * The raw token is never written to the database.
         */
        String rawRefreshToken = generateRefreshToken();

        String refreshTokenHash =
                hashToken(rawRefreshToken);

        Instant refreshTokenExpiresAt =
                Instant.now().plus(refreshTokenTtl);

        RefreshToken refreshToken = RefreshToken.create(
                authenticatedUser,
                refreshTokenHash,
                refreshTokenExpiresAt
        );

        refreshTokenRepository.saveAndFlush(refreshToken);

        Instant accessTokenExpiresAt =
                Instant.now().plus(
                        jwtService.getAccessTokenTtl()
                );

        IdentityResponse identityResponse =
                identityMapper.toIdentityResponse(
                        authenticatedUser
                );

        return new AuthenticationResult(
                identityResponse,
                accessToken,
                rawRefreshToken,
                accessTokenExpiresAt,
                refreshTokenExpiresAt
        );
    }

    /*
     * ============================================================
     * REFRESH-TOKEN ROTATION
     * ============================================================
     */

    @Transactional(
            noRollbackFor = AuthenticationFailedException.class
    )
    public AuthenticationResult refresh(String rawRefreshToken) {

        if (rawRefreshToken == null
                || rawRefreshToken.isBlank()) {

            throw new AuthenticationFailedException();
        }

        /*
         * Only the hash is used for the database lookup.
         */
        String submittedTokenHash =
                hashToken(rawRefreshToken);

        Optional<RefreshToken> tokenOptional =
                refreshTokenRepository.findByTokenHashForUpdate(
                        submittedTokenHash
                );

        if (tokenOptional.isEmpty()) {

            throw new AuthenticationFailedException();
        }

        RefreshToken currentToken = tokenOptional.get();

        BankUser user = currentToken.getUser();

        Instant now = Instant.now();

        /*
         * A revoked token must never be accepted again.
         *
         * Reuse can indicate that an attacker has obtained an old
         * refresh token. Revoke the user's remaining active sessions.
         */
        if (currentToken.getRevokedAt() != null) {

            revokeAllActiveRefreshTokens(user);

            refreshTokenRepository.flush();

            throw new AuthenticationFailedException();
        }

        /*
         * Expired refresh tokens are invalid.
         * Persist their revocation before rejecting the request.
         */
        if (!currentToken.getExpiresAt().isAfter(now)) {

            currentToken.setRevokedAt(now);

            refreshTokenRepository.saveAndFlush(currentToken);

            throw new AuthenticationFailedException();
        }

        /*
         * Check current account state before issuing a replacement.
         */
        if (!user.isEnabled() || !user.isEmailVerified()) {

            revokeAllActiveRefreshTokens(user);

            refreshTokenRepository.flush();

            throw new AuthenticationFailedException();
        }

        /*
         * An expired timed lock may be cleared.
         * An indefinite or active lock prevents refresh.
         */
        if (!user.isAccountNonLocked()) {

            Instant lockedUntil = user.getLockedUntil();

            if (lockedUntil != null
                    && !lockedUntil.isAfter(now)) {

                user.setAccountNonLocked(true);

                user.setLockedUntil(null);

                user.setFailedLoginAttempts(0);

                bankUserRepository.saveAndFlush(user);

            } else {

                revokeAllActiveRefreshTokens(user);

                refreshTokenRepository.flush();

                throw new AuthenticationFailedException();
            }
        }

        /*
         * Generate the replacement refresh token.
         */
        String replacementRawToken =
                generateRefreshToken();

        String replacementTokenHash =
                hashToken(replacementRawToken);

        Instant replacementExpiresAt =
                now.plus(refreshTokenTtl);

        RefreshToken replacementToken =
                RefreshToken.create(
                        user,
                        replacementTokenHash,
                        replacementExpiresAt
                );

        /*
         * Persist the replacement before linking the old record
         * to it, so the replacement has a database identity.
         */
        RefreshToken savedReplacement =
                refreshTokenRepository.saveAndFlush(
                        replacementToken
                );

        /*
         * Revoke the old token and record the replacement link.
         */
        currentToken.setRevokedAt(now);

        currentToken.setReplacedByToken(
                savedReplacement
        );

        refreshTokenRepository.saveAndFlush(
                currentToken
        );

        /*
         * Issue a new access token.
         */
        String accessToken =
                jwtService.generateAccessToken(user);

        Instant accessTokenExpiresAt =
                Instant.now().plus(
                        jwtService.getAccessTokenTtl()
                );

        IdentityResponse identityResponse =
                identityMapper.toIdentityResponse(user);

        return new AuthenticationResult(
                identityResponse,
                accessToken,
                replacementRawToken,
                accessTokenExpiresAt,
                replacementExpiresAt
        );
    }

    /*
     * ============================================================
     * LOGOUT
     * ============================================================
     */

    @Transactional
    public void logout(String rawRefreshToken) {

        /*
         * Logout is idempotent.
         * Missing, malformed, or unknown refresh tokens do not
         * prevent the controller from clearing the cookies.
         */
        if (rawRefreshToken == null
                || rawRefreshToken.isBlank()) {

            return;
        }

        String tokenHash = hashToken(rawRefreshToken);

        Optional<RefreshToken> tokenOptional =
                refreshTokenRepository.findByTokenHashForUpdate(
                        tokenHash
                );

        if (tokenOptional.isEmpty()) {
            return;
        }

        RefreshToken refreshToken = tokenOptional.get();

        if (refreshToken.getRevokedAt() == null) {

            refreshToken.setRevokedAt(
                    Instant.now()
            );

            refreshTokenRepository.saveAndFlush(
                    refreshToken
            );
        }
    }

    /*
     * ============================================================
     * PRIVATE HELPERS
     * ============================================================
     */

    private void revokeAllActiveRefreshTokens(BankUser user) {

        List<RefreshToken> tokens =
                refreshTokenRepository.findAllByUser_Id(
                        user.getId()
                );

        Instant now = Instant.now();

        for (RefreshToken token : tokens) {

            if (token.getRevokedAt() == null) {

                token.setRevokedAt(now);
            }
        }

        refreshTokenRepository.saveAllAndFlush(tokens);
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {

            throw new AuthenticationFailedException();
        }

        return email.trim()
                .toLowerCase(Locale.ROOT);
    }

    private String generateRefreshToken() {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String token) {

        try {

            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = messageDigest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

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
}