package com.gideonheritagebank.IDENTITY_SERVICE.service;

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

    @Value("${identity.security.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${identity.security.login.lock-duration:PT15M}")
    private Duration lockDuration;

    @Value("${identity.jwt.refresh-token-ttl:P30D}")
    private Duration refreshTokenTtl;

    private final SecureRandom secureRandom = new SecureRandom();

    /*
     * Used when an email address does not exist.
     * Performing a password-hash comparison in that case helps reduce
     * obvious timing differences between unknown emails and wrong passwords.
     */
    private String dummyPasswordHash;

    @PostConstruct
    void initializeDummyPasswordHash() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String randomDummyPassword = Base64.getEncoder()
                .encodeToString(randomBytes);

        dummyPasswordHash = passwordEncoder.encode(randomDummyPassword);
    }

    /**
     * Authenticates a user and creates an access token and refresh-token record.
     * AuthenticationFailedException deliberately does not trigger rollback:
     * failed-login counter and lockout changes must be committed.
     * The returned refresh token is a raw secret for the controller to set
     * in a cookie. Only its SHA-256 hash is persisted.
     */
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthenticationResult login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword();

        Optional<BankUser> userOptional = bankUserRepository.findByEmailForUpdate(email);

        if (userOptional.isEmpty()) {

            passwordEncoder.matches(
                    rawPassword,
                    dummyPasswordHash
            );

            throw new AuthenticationFailedException();
        }

        BankUser user = userOptional.get();

        /*
         * Verify the password before returning an account-state error.
         * All authentication failures use the same public exception.
         */
        boolean passwordMatches = passwordEncoder.matches(
                rawPassword,
                user.getPasswordHash()
        );

        Instant now = Instant.now();

        /*
         * Clear an expired timed lock.
         *
         * A locked account with no lockedUntil timestamp is treated as
         * administratively locked and will not automatically unlock.
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
         * These checks prevent disabled or unverified accounts from
         * obtaining new access tokens.
         */
        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new AuthenticationFailedException();
        }

        /*
         * Invalid password: record the failure and apply the lockout policy.
         */
        if (!passwordMatches) {

            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            if (failedAttempts >= maxFailedAttempts) {

                user.setAccountNonLocked(false);
                user.setLockedUntil(now.plus(lockDuration));
            }

            bankUserRepository.saveAndFlush(user);

            throw new AuthenticationFailedException();
        }

        /*
         * Successful authentication:
         * reset the counters and clear any stale lock metadata.
         */
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockedUntil(null);

        BankUser authenticatedUser = bankUserRepository.saveAndFlush(user);

        /*
         * Generate the signed access token.
         */
        String accessToken = jwtService.generateAccessToken(authenticatedUser);

        /*
         * Generate a cryptographically random refresh token.
         * The raw token is returned to the caller but is never stored.
         */
        String rawRefreshToken = generateRefreshToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        Instant refreshTokenExpiresAt = Instant.now().plus(refreshTokenTtl);

        RefreshToken refreshToken = RefreshToken.create(
                authenticatedUser,
                refreshTokenHash,
                refreshTokenExpiresAt
        );

        refreshTokenRepository.saveAndFlush(refreshToken);

        Instant accessTokenExpiresAt = Instant.now().plus(jwtService.getAccessTokenTtl());

        return new AuthenticationResult(
                identityMapper.toIdentityResponse(authenticatedUser),
                accessToken,
                rawRefreshToken,
                accessTokenExpiresAt,
                refreshTokenExpiresAt
        );
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new AuthenticationFailedException();
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateRefreshToken() {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] hash = messageDigest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (NoSuchAlgorithmException exception) {

            /*
             * SHA-256 is required by Java, so this should never occur
             * in a correctly functioning JVM.
             */
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    /**
     * Internal authentication result.
     * Do not return this record directly from a REST controller:
     * that would expose the raw access and refresh tokens in the JSON body.
     * The authentication controller will use the tokens to set cookies
     * and return only the identity and other safe response information.
     */
    public record AuthenticationResult(
            IdentityResponse identity,
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt
    ) {
    }
}