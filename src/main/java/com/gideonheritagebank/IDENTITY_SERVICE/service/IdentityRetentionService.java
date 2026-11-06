package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.repository.EmailVerificationTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.PasswordResetTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityRetentionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${identity.retention.auth-token-record-period:P90D}")
    private Duration authenticationTokenRecordPeriod;

    @Scheduled(
            cron = "${identity.retention.cleanup-cron:0 0 3 * * *}",
            zone = "Africa/Lagos"
    )
    @Transactional
    public void purgeOldAuthenticationTokenRecords() {

        if (authenticationTokenRecordPeriod == null
                || authenticationTokenRecordPeriod.isNegative()
                || authenticationTokenRecordPeriod.isZero()) {

            log.error("Authentication token retention period is invalid. Cleanup skipped.");

            return;
        }

        Instant cutoff = Instant.now().minus(authenticationTokenRecordPeriod);
        int refreshTokensDeleted = refreshTokenRepository.deleteOldTokenRecords(cutoff);
        int verificationTokensDeleted = emailVerificationTokenRepository.deleteOldTokenRecords(cutoff);
        int passwordResetTokensDeleted = passwordResetTokenRepository.deleteOldTokenRecords(cutoff);

        log.info(
                "Authentication-token retention cleanup completed. " +
                        "refreshTokens={}, verificationTokens={}, passwordResetTokens={}",
                refreshTokensDeleted,
                verificationTokensDeleted,
                passwordResetTokensDeleted
        );
    }
}